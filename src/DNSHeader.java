import java.io.*;

public class DNSHeader {
    /*
    This class should store all the data provided by the 12 byte DNS header.
     */

    private int qdCount, anCount, nsCount, arCount; // actually unsigned short in the form of int
    private short id;
    private byte qr, opCode, aa, tc, rd, ra, z, ad, cd, rCode;

    /**
     * Read the header from an input stream
     * @return the DNSHeader object
     */

    public static DNSHeader decodeHeader(InputStream in) throws IOException {
        DNSHeader header = new DNSHeader();
        DataInputStream inputStream = new DataInputStream(in);
        header.id = inputStream.readShort();

        int thirdByte = inputStream.readUnsignedByte();
        byte[] thirdByteParts = Helpers.dissectByte(thirdByte, new byte[] {1, 4, 1, 1, 1});
        header.qr = thirdByteParts[0];
        header.opCode = thirdByteParts[1];
        header.aa = thirdByteParts[2];
        header.tc = thirdByteParts[3];
        header.rd = thirdByteParts[4];

        int fourthByte = inputStream.readUnsignedByte();
        byte[] fourthByteParts = Helpers.dissectByte(fourthByte, new byte[] {1, 1, 1, 1, 4});
        header.ra = fourthByteParts[0];
        header.z = fourthByteParts[1];
        header.ad = fourthByteParts[2];
        header.cd = fourthByteParts[3];
        header.rCode = fourthByteParts[4];

        header.qdCount = inputStream.readUnsignedShort();
        header.anCount = inputStream.readUnsignedShort();
        header.nsCount = inputStream.readUnsignedShort();
        header.arCount = inputStream.readUnsignedShort();

        return header;
    }

    /**
     * This will create the header for the response. It will copy some fields from the request
     * @param request the DNS request message
     * @param response the DNS response message
     * @return the DNS response message
     */
    public static DNSHeader buildResponseHeader(DNSMessage request, DNSMessage response) {
        DNSHeader reqHeader = request.getHeader();
        DNSHeader resHeaderFromGoogle = response.getHeader();
        DNSHeader resHeader = new DNSHeader();

        resHeader.id = reqHeader.id;
        resHeader.qr = 1;
        resHeader.opCode = reqHeader.opCode;
        resHeader.aa = 0;
        resHeader.tc = 0;
        resHeader.rd = reqHeader.rd;
        resHeader.ra = 0;
        resHeader.z = 0;
        resHeader.rCode = 0; // change if exceptions thrown

        resHeader.qdCount = reqHeader.qdCount;
        resHeader.anCount = 1; // we'll be only returning one answer
        resHeader.nsCount = 0; // ignore authority records as per instructions
        resHeader.arCount = reqHeader.arCount;

        return resHeader;
    }

    /**
     * Encode the header to bytes to be sent back to the client.
     * @param out the output stream
     * @throws IOException on output stream errors
     */
    public void writeBytes(OutputStream out) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(out);

        outputStream.writeShort(this.id);

        byte thirdByte = Helpers.mergeIntoByte(new byte[] {qr, opCode, aa, tc, rd},
                                                 new byte[] {1, 4, 1, 1, 1} );
        outputStream.writeByte(thirdByte);

        byte fourthByte = Helpers.mergeIntoByte(new byte[] {ra, z, ad, cd, rCode},
                                                  new byte[] {1, 1, 1, 1, 4});
        outputStream.writeByte(fourthByte);

        outputStream.writeShort(qdCount);
        outputStream.writeShort(anCount);
        outputStream.writeShort(nsCount);
        outputStream.writeShort(arCount);
    }

    /**
     * @return a human readable string version of a header object
     */
    @Override
    public String toString() {
        return String.format("""
                        Header object:
                        id: %x, qr: %x, opCode: %x, aa: %x, tc: %x, rd: %x, ra: %x, z: %x, ad: %x, cd: %x, rCode: %x
                        qdCount: %x, anCount: %x, nsCount: %x, arCount: %x""",
                id, qr, opCode, aa, tc, rd, ra, z, ad, cd, rCode, qdCount, anCount, nsCount, arCount);
    }

    public boolean isQuery() {
        return qr == 0;
    }

    public boolean isErrorResponse() {
        return rCode != 0;
    }
}
