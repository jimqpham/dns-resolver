import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Helpers {

    /**
     * Cut the byte into sections of bits
     * @param unsignedByteToDissect the unsigned byte (in the form of int) to cut up
     * @param numBitsInSections an array containing number of bits in each section. For example, [2, 3, 3]
     *                          means to cut the byte into 3 sections of 2 bits, 3 bits and 3 bits accordingly.
     *                          Must add up to 8.
     * @return The array of bit sections
     */
    public static byte[] dissectByte (int unsignedByteToDissect, byte[] numBitsInSections) {
        int numBitsPresent = 0;
        for (byte b: numBitsInSections)
            numBitsPresent += b;
        if (numBitsPresent != 8)
            throw new IllegalArgumentException();

        byte[] dissectedBytes = new byte[numBitsInSections.length];

        for (int i = numBitsInSections.length - 1; i >= 0; i--) {
            byte numBitsInThisSection = numBitsInSections[i];
            byte dissectedByte = (byte) (unsignedByteToDissect & ((byte) Math.pow(2, numBitsInThisSection) - 1));
            dissectedBytes[i] = dissectedByte;
            unsignedByteToDissect = (byte) (unsignedByteToDissect >> numBitsInThisSection);
        }

        return dissectedBytes;
    }

    /**
     * The opposite of the dissectByte().
     * This method reads in two byte arrays: the first one containing the value of each byte section, and the second one
     * containing the length of each byte section. It then merges the section together to create an 8-bit byte
     * @param byteSections an array containing the values of byte sections
     * @param numBitsInSections an array containing the lengths of byte sections
     * @return a merged 8-but byte
     */
    public static byte mergeIntoByte (byte[] byteSections, byte[] numBitsInSections) {
        // ERROR CHECKING 1
        if (byteSections.length != numBitsInSections.length)
            throw new IllegalArgumentException();

        // ERROR CHECKING 2
        int numBitsPresent = 0;
        for (byte b: numBitsInSections)
            numBitsPresent += b;
        if (numBitsPresent != 8)
            throw new IllegalArgumentException();

        byte returnedByte = 0;
        byte remainingBits = 8;
        for (int i = 0; i < byteSections.length; i++) {
            byte section = byteSections[i];
            byte numBitsInThisSection = numBitsInSections[i];
            returnedByte = (byte) (returnedByte | (section << (remainingBits - numBitsInThisSection)));
            remainingBits -= numBitsInThisSection;
        }

        return  returnedByte;
    }

    /**
     * Check if an octet is a pointer to another address
     * @param octet the byte/octet to be inspected
     * @return true if the octet represents a pointer (two most significant bits are ones)
     */
    public static boolean isPointer (byte octet) {
        byte[] octetParts = Helpers.dissectByte(octet, new byte[] {2, 6});
        byte twoMSBs = octetParts[0];
        return twoMSBs == 0x03;
    }

    /**
     * Chop off two most significant bits of a pointer octet and read the address (6 remaining bits)
     * @param firstPtrOctet The first byte/octet
     * @param secondPtrOctet The second byte/octet
     * @return the value the octet is pointing to
     */
    public static int readPointerAddress (byte firstPtrOctet, byte secondPtrOctet) {
        int firstPart = dissectByte(firstPtrOctet, new byte[] {2, 6})[1];
        int secondPart = secondPtrOctet & 0x0ff;
        return ((firstPart << 8) | secondPart) & 0x0ffff;
    }

    /**
     * Write a full domain name to the packet if the domain name has not been written previously, or a pointer if otherwise
     * @param outputStream the output stream
     * @param labels the labels making up the domain name
     * @param domainNameLocations the hashmap for looking up previous locations of the domain name
     */
    public static void writeDomainNames (ByteArrayOutputStream outputStream,
                                         String[] labels,
                                         HashMap<String,Integer> domainNameLocations) {
        Integer prevLoc = domainNameLocations.get(Helpers.octetsToString(labels));
        if (prevLoc == null) {
            domainNameLocations.put(Helpers.octetsToString(labels), outputStream.size());
            for (String label : labels) {
                int labelLength = label.length();
                outputStream.write(labelLength);
                outputStream.writeBytes(label.getBytes(StandardCharsets.UTF_8));
            }
            outputStream.write(0);
        }
        else {
            int pointerAddress = prevLoc | 0x0000c000; // change two MSBs to ones
            byte pointerAddressByteOne = (byte) (pointerAddress >> 8);
            byte pointerAddressByteTwo = (byte) (pointerAddress);
            outputStream.writeBytes(new byte[] {pointerAddressByteOne, pointerAddressByteTwo});
        }
    }

    /**
     * Join the pieces of a domain name with dots ([ "utah", "edu"] -> "utah.edu" )
     * @param octets pieces of a domain name
     * @return the concatenated domain name string
     */
    public static String octetsToString(String[] octets) {
        StringBuilder concatStr = new StringBuilder();
        for (int i = 0; i < octets.length - 1; i++) {
            concatStr.append(octets[i]);
            concatStr.append('.');
        }
        concatStr.append(octets[octets.length - 1]);
        return concatStr.toString();
    }
}
