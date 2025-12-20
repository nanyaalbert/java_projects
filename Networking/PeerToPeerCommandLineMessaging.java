import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class PeerToPeerCommandLineMessaging {
    public static void main(String[] args) {
        ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
        Scanner userInput = new Scanner(System.in);
        String incomingMessage;
        String outgoingMessage;
        final String HANDSHAKE = "P2PCLIChat";
        final String MESSAGESIGNAL = "1";
        final String EXITSIGNAL = "0";
        System.out.println("Welcome to the Peer to Peer Command Line Messaging");
        System.out.println("Do you want to start as \n\t1. Server\n\t2. Client");
        System.out.println("Enter a number that corresponds to your choice");
        mainloop: while (true) {
            switch (userInput.nextLine()) {
                case "1" -> {
                    final int PORT = 1234;
                    try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
                        serverChannel.bind(new InetSocketAddress(PORT));
                        serverChannel.configureBlocking(false);
                        System.out.println("Server currently listening on port " + PORT);
                        System.out.println("Waiting for connections...");
                        innerloop: while (true) {
                            SocketChannel socketChannel = serverChannel.accept();
                            if (socketChannel == null)
                                Thread.sleep(2000);
                            else {
                                socketChannel.write(sendBuffer.clear().put(HANDSHAKE.getBytes(StandardCharsets.UTF_8)));
                                socketChannel.read(receiveBuffer);
                                if (!StandardCharsets.UTF_8.decode(receiveBuffer.flip()).toString().equals(HANDSHAKE)) {
                                    System.out.println("Connected program is not PeerToPeerCommandLineMessaging");
                                    System.out.println("Disconnecting...");
                                    socketChannel.close();
                                    System.out.println("Disconnected");
                                    System.out.println("Waiting for new connections...");
                                    continue innerloop;
                                }
                                System.out.println("Connected...");
                                System.out.println(
                                        "Enter your message and hit enter. if you want to quit, simply type 'quit'");
                                while (true) {
                                    outgoingMessageloop: while (true) {
                                        System.out.print("SEND: ");
                                        outgoingMessage = userInput.nextLine();
                                        if (outgoingMessage.length() > 0) {
                                            if (outgoingMessage.equalsIgnoreCase("quit")) {
                                                sendBuffer.clear().put(EXITSIGNAL.getBytes(StandardCharsets.UTF_8));
                                                while (sendBuffer.hasRemaining()) {
                                                    sendBuffer.flip();
                                                    socketChannel.write(sendBuffer);
                                                }
                                                System.out.println("Quitting...");
                                                break mainloop;
                                            } else {
                                                sendBuffer.clear().put((MESSAGESIGNAL + outgoingMessage)
                                                        .getBytes(StandardCharsets.UTF_8));
                                                while (sendBuffer.hasRemaining()) {
                                                    sendBuffer.flip();
                                                    socketChannel.write(sendBuffer);
                                                }
                                                System.out.println();
                                                break outgoingMessageloop;
                                            }
                                        } else {
                                            System.out.println("You cannot send an empty message, try again");
                                            continue outgoingMessageloop;
                                        }
                                    }
                                    System.out.print("RECEIVE: ");
                                    int bytesRead = socketChannel.read(receiveBuffer.clear());
                                    if (bytesRead < 0) {
                                        System.out.println("Connection closed at the other end");
                                        socketChannel.close();
                                        System.out.println("Waiting for new connections...");
                                        continue innerloop;
                                    } else {
                                        receiveBuffer.flip();
                                        incomingMessage = StandardCharsets.UTF_8.decode(receiveBuffer).toString();
                                        receiveBuffer.clear();
                                        System.out.print(incomingMessage);
                                        System.out.println();
                                    }
                                }

                            }
                        }
                    } catch (Exception e) {
                        System.out.println("An error occured during server setup: " + e.getMessage());
                    }
                }
                case "2" -> {
                }
                default -> {
                    System.out.println("Please enter a '1' or '2'");
                    continue mainloop;
                }
            }
        }
    }

}
