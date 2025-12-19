import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RemoteLoggingServiceServer {
    private static final int PORT = 1234;
    private static final String logFileName = "remote_log.txt";
    private static Path logFilePath = Paths.get(logFileName);

    public static void main(String[] args) {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.bind(new InetSocketAddress("localhost", PORT));
            System.out.println("Server listening on port " + PORT);
            while (true) {
                System.out.println("Waiting for connections...");
                SocketChannel clientChannel = serverChannel.accept();
                if (clientChannel == null)
                    Thread.sleep(2000);
                else {
                    System.out.println(
                            "Server successfully connected to client: " + clientChannel.getRemoteAddress().toString());
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    StringBuilder message = new StringBuilder();
                    while (clientChannel.read(buffer) > 0) {
                        buffer.flip();
                        String text = new String(
                                buffer.array(),
                                buffer.arrayOffset() + buffer.position(),
                                buffer.remaining(),
                                StandardCharsets.UTF_8);
                        message.append(text);
                        buffer.clear();
                    }
                    System.out
                            .println("Log entry received from client: " + clientChannel.getRemoteAddress().toString());
                    String logEntry = String.format("\n[%s] : [%s] ", clientChannel.getRemoteAddress().toString(),
                            message.toString());
                    System.out.println("Writing log to file");

                    try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(logFilePath,
                            StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                        fileChannel.write(ByteBuffer.wrap(logEntry.getBytes()), fileChannel.size(), null,
                                new CompletionHandler<Integer, Void>() {

                                    @Override
                                    public void completed(Integer result, Void attachment) {
                                        System.out.println("Log Entry: [" + logEntry + "] written to file");
                                    }

                                    @Override
                                    public void failed(Throwable exc, Void attachment) {
                                        System.out
                                                .println("Log Entry: [" + logEntry + "] could not be written to file");
                                    }

                                });
                    } catch (Exception e) {
                        System.out.println("An error occured while writing log to file " + e.getMessage());
                    }
                }

            }
        } catch (Exception e) {
            System.out.println("An error was encountered while setting up the server");
        }
    }

}
