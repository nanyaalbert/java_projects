import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class RemoteLoggingServiceClient {
    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress("localhost", PORT));
            socketChannel.finishConnect();
            if (socketChannel.isConnected()) {
                Scanner userInput = new Scanner(System.in);
                String logLevel;
                String logMessage;
                System.out.println("Successfully connected to server...");
                System.out.println("Enter log level(e.g. high, low, medium): ");
                logLevel = userInput.nextLine();
                System.out.println("Enter log message: ");
                logMessage = userInput.nextLine();

                String logEntry = String.format("[%s] : [%s] - %s",
                        java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        logLevel.toUpperCase(),
                        logMessage);

                socketChannel.write(ByteBuffer.wrap(logEntry.getBytes()));
                userInput.close();

            } else
                System.out.println("Could not connect to server...");

        } catch (Exception e) {
            System.out.println("An error occured while trying to connect to server: " + e.getMessage());
        }
    }

}
