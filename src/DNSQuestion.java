import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

public class DNSQuestion {

    String[] labels;
    int qType, qClass;

    /**
     * Read a question from the input stream.
     * @return A DNSQuestion object
     */
    public static DNSQuestion decodeQuestion(InputStream in, DNSMessage dnsMess) throws IOException {
        DNSQuestion question = new DNSQuestion();

        DataInputStream inputStream = new DataInputStream(in);

        question.labels = dnsMess.readDomainName(inputStream);
        question.qType = inputStream.readUnsignedShort();
        question.qClass = inputStream.readUnsignedShort();

        return question;
    }

    /**
     * Write the question bytes which will be sent to the client.
     * @param byteArrOut the byte output stream
     * @param domainNameLocations the hash map which is used to compress the message
     * @throws IOException on output stream errors
     */
    public void writeBytes(ByteArrayOutputStream byteArrOut, HashMap<String,Integer> domainNameLocations)
            throws IOException {

        DataOutputStream outputStream = new DataOutputStream(byteArrOut);

        Helpers.writeDomainNames(byteArrOut, this.labels, domainNameLocations);
        outputStream.writeShort(qType);
        outputStream.writeShort(qClass);
    }

    /**
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "Question object:\nLabels: " + Arrays.toString(labels) + "\nqType: " + qType + ", qClass: " + qClass;
    }

    /**
     * @param o the object to compare
     * @return true on equality and false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof DNSQuestion)
            return Arrays.equals(labels, ((DNSQuestion) o).labels) &&
                    qClass == ((DNSQuestion) o).qClass &&
                    qType == ((DNSQuestion) o).qType;

        return false;
    }

    /**
     * For hashmap lookup. Use the hashcode of the domain name instead
     * @return The hashcode of the domain name
     */
    @Override
    public int hashCode() {
        return Helpers.octetsToString(labels).hashCode();
    }

    /**
     * Merge the pieces of the domain name together
     * @return a concatenated string representation of the domain name
     */
    public String getDomainNameAsString() {
        return Helpers.octetsToString(labels);
    }
}
