import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class DNSRecord {


    private String[] labels;
    private int rType, rClass;
    private byte[] rData;
    private long ttl; // actually an unsigned integer
    private Date timestamp;

    /**
     * Decode the input stream into a DNS record
     * @param in the input stream
     * @param dnsMess the encapsulating DNS message for de-compression of domain names
     * @return a DNS record
     */
    public static DNSRecord decodeRecord(InputStream in, DNSMessage dnsMess) throws IOException {
        DNSRecord record = new DNSRecord();
        DataInputStream inputStream = new DataInputStream(in);

        record.labels = dnsMess.readDomainName(inputStream);

        record.rType = inputStream.readUnsignedShort();

        record.rClass = inputStream.readUnsignedShort();

        record.ttl = (inputStream.readInt()) & 0xffffffffL; // read as unsigned

        int rdLength = inputStream.readUnsignedShort();
        record.rData = inputStream.readNBytes(rdLength);

        record.timestamp = new Date();

        return record;
    }

    /**
     * Encode the DNS record to bytes and send back
     * @param byteArrOut the byte array output stream
     * @param lookupTable the table to check if domain names should be compressed
     */
    public void writeBytes(ByteArrayOutputStream byteArrOut, HashMap<String, Integer> lookupTable) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(byteArrOut);

        Helpers.writeDomainNames(byteArrOut, labels, lookupTable);
        outputStream.writeShort(rType);
        outputStream.writeShort(rClass);
        outputStream.writeInt((int) ttl);
        outputStream.writeShort(rData.length);
        for (int piece: rData) {
            outputStream.writeByte(piece);
        }
    }

    /**
     * @return a human readable string rerpresentation of the DNS record
     */
    public String toString() {

        StringBuilder rDataString = new StringBuilder("[");
        for (byte b: rData) {
            rDataString.append(b & 0x00ff);
            rDataString.append(", ");
        }
        rDataString.append(']');

        return "Record object:\n" +
                "Labels: " + Arrays.toString(labels) +
                "\nrType" + rType + ", rClass: " + rClass + ", rData: " + rDataString + ", ttl: " + ttl;
    }

    /**
     * @return whether the creation date + the time to live is after the current time.
     * The Date and Calendar classes will be useful for this.
     */
    public boolean timestampValid() {
        Date current = new Date();
        return (this.timestamp.getTime()/1000 + ttl) > current.getTime()/1000;
    }
}
