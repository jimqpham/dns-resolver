import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DNSMessage {

    private DNSHeader header;
    private DNSQuestion[] questions;
    private DNSRecord[] answers;
    private DNSRecord[] authorityRecords;
    private byte[] additionalRecords;
    private byte[] bytes;

    /**
     * Decode a byte array into a DNS message
     * @param bytes byte array to be decode
     * @return a DNSMessage object
     */
    public static DNSMessage decodeMessage(byte[] bytes) throws IOException {
        InputStream in = new ByteArrayInputStream(bytes);
        DNSMessage message = new DNSMessage();

        message.bytes = bytes;

        message.header = DNSHeader.decodeHeader(in);

        message.questions = new DNSQuestion[] {DNSQuestion.decodeQuestion(in, message)};

        if (!message.isQuery())
            message.answers = new DNSRecord[] {DNSRecord.decodeRecord(in, message)};

        message.additionalRecords = in.readNBytes(11);

        return message;
    }

    /**
     * @param in the input stream
     * @return the pieces of a domain name starting from the current position of the input stream
     */
    public String[] readDomainName(InputStream in) throws IOException {
        DataInputStream inputStream = new DataInputStream(in);
        byte nextOctet = inputStream.readByte();

        // use ArrayList for dynamic sizing when reading in labels
        // will convert back to String[] at the end
        ArrayList<String> labels = new ArrayList<>();

        boolean hitZeroOctet = false;
        boolean hitPointer = false;
        while (!hitZeroOctet && !hitPointer) {
            if (nextOctet == 0)
                hitZeroOctet = true;
            else if (Helpers.isPointer(nextOctet))
                hitPointer = true;
            else {
                byte[] labelByteArr = inputStream.readNBytes(nextOctet);
                labels.add(new String(labelByteArr));
                nextOctet = inputStream.readByte();
            }
        }

        if (Helpers.isPointer(nextOctet)) {
            int pointerAddress = Helpers.readPointerAddress(nextOctet, inputStream.readByte());
            String[] pointerLabels = this.readDomainName(pointerAddress);
            labels.addAll(Arrays.asList(pointerLabels));
        }

        return labels.toArray(new String[0]);
    }

    /**
     * Used when there's compression, and we need to find the domain from earlier in the message.
     * @param firstByte byte index of the address being pointed to by the pointer
     * @return the compressed domain name
     */
    public String[] readDomainName(int firstByte) throws IOException {
        byte[] slicedBytes = Arrays.copyOfRange(this.bytes, firstByte, this.bytes.length);
        InputStream slicedInputStream = new ByteArrayInputStream(slicedBytes);
        return readDomainName(slicedInputStream);
    }

    /**
     * Build a response based on the request and the answers you intend to send back.
     * @param request the DNS request message
     * @param answers the answer (one answer) to send back
     * @return the DNS response message
     */
    public static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers) {
        DNSMessage response = new DNSMessage();

        response.header = DNSHeader.buildResponseHeader(request, response);

        response.questions = request.questions;

        response.answers = answers;

        response.additionalRecords = request.additionalRecords;

        return response;
    }

    /**
     * @return the byte array to be put in a packet and sent back
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream byteArrOut = new ByteArrayOutputStream(512);

        HashMap<String, Integer> domainNameLocations = new HashMap<>();

        this.header.writeBytes(byteArrOut);
        this.questions[0].writeBytes(byteArrOut, domainNameLocations);
        this.answers[0].writeBytes(byteArrOut, domainNameLocations);
        byteArrOut.writeBytes(additionalRecords);

        return byteArrOut.toByteArray();
    }

    /**
     * Logging the message to the screen - for error checking
     * @return A string representation of the DNS Message
     */
    public String toString() {
        return header.toString() + "\n\n" + Arrays.toString(questions) + "\n\n" + Arrays.toString(answers) + "\n\n";
    }

    public DNSHeader getHeader() {
        return header;
    }

    public DNSQuestion[] getQuestions() {
        return questions;
    }

    public DNSRecord[] getAnswers() {
        return answers;
    }

    public boolean isQuery() {
        return this.header.isQuery();
    }
}
