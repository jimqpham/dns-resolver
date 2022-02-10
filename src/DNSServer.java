import java.io.IOException;
import java.net.*;

public class DNSServer {

    public static void main(String[] args) {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(8053);
        }
        catch (SocketException soe) {
            System.out.println("Socket cannot be opened.");
        }
        catch (SecurityException see) {
            System.out.println("Unauthorized access to socket.");
        }
        catch (IllegalArgumentException iae) {
            System.out.println("Socket number out of range.");
        }

        System.out.println("Socket is up on port 8053\n");

        while (true) {
            try {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                System.out.println(">>>>>> Receiving DNS request");
                int digPort = packet.getPort();

                DNSMessage reqMessage = DNSMessage.decodeMessage(packet.getData());
                DNSQuestion question = reqMessage.getQuestions()[0];
                DNSRecord answer = DNSCache.lookup(question);

                if (answer != null) {
                    sendResponse(reqMessage, answer, socket, digPort);
                } else {
                    DNSMessage googleResponseMessage = consultGoogle(packet);
                    if (googleResponseMessage.getHeader().isErrorResponse()) {
                        // If the response from Google contains an error (e.g. domain name not exist)
                        // then skip caching and relay response verbatim to requester
                        forwardResponseFromGoogle(googleResponseMessage, socket, digPort);
                    } else {
                        // Cache and send a normal response
                        DNSCache.insert(question, googleResponseMessage.getAnswers()[0]);
                        sendResponse(reqMessage, googleResponseMessage.getAnswers()[0], socket, digPort);
                    }
                }
            }
            catch (IOException ioe) {
                System.out.println("Something's wrong with the data stream...");
            }
        }
    }

    /**
     * Send back a normal response
     * @param reqMessage the DNSMessage object containing the request
     * @param answer the answer to send back
     * @param socket the socket that dig sends the request to
     * @param digPort the port that dig is listening on
     * @throws IOException when there's an error with the stream
     */
    private static void sendResponse (DNSMessage reqMessage, DNSRecord answer, DatagramSocket socket, int digPort) throws IOException {
        System.out.println("\tSending back answer...");
        DNSMessage resMessage = DNSMessage.buildResponse(reqMessage, new DNSRecord[] {answer});
        byte[] resByteArr = resMessage.toBytes();
        DatagramPacket resPacket = new DatagramPacket(resByteArr, resByteArr.length, InetAddress.getByName("127.0.0.1"), digPort);
        socket.send(resPacket);
        System.out.println(">>>>>> Finish sending response\n");
    }

    /**
     * Forward the request to Google and get back a response
     * @param packet the request packet
     * @return A DNSMessage object  containing info from Google
     * @throws IOException when there's an error with the stream
     */
    private static DNSMessage consultGoogle(DatagramPacket packet) throws IOException {
        System.out.println("\tQuerying Google...");
        DatagramSocket fwdSocket = new DatagramSocket();
        DatagramPacket fwdPacket = new DatagramPacket(packet.getData(), packet.getLength(), InetAddress.getByName("8.8.8.8"), 53);
        fwdSocket.send(fwdPacket);

        byte[] buf = new byte[512]; // new buffer because fwdPacket's buffer is not big enough for the answer
        DatagramPacket rcvPacket = new DatagramPacket(buf, buf.length);
        fwdSocket.receive(rcvPacket);
        return DNSMessage.decodeMessage(rcvPacket.getData());
    }

    /**
     * Used when Google sends back an error response (e.g. when domain name does not exist)
     * This method skips caching and just relays the Google response back to the requester
     * @param googleResponse A DNSMessage object containing the response from Google
     * @param socket the socket that dig initially sent the request to
     * @param digPort the port dig is listening on
     * @throws IOException when there's an error with the stream
     */
    private static void forwardResponseFromGoogle (DNSMessage googleResponse, DatagramSocket socket, int digPort) throws IOException {
        System.out.println("\tSending back answer...");
        byte[] resByteArr = googleResponse.toBytes();
        DatagramPacket resPacket = new DatagramPacket(resByteArr, resByteArr.length, InetAddress.getByName("127.0.0.1"), digPort);
        socket.send(resPacket);
        System.out.println(">>>>>> Finish sending response\n");
    }
}
