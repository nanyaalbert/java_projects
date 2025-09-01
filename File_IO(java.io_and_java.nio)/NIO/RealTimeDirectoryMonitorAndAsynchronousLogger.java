import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RealTimeDirectoryMonitorAndAsynchronousLogger monitors a specified directory
 * for file creation, modification, and deletion events in real-time.
 * It logs these events asynchronously to a log file with timestamps.
 * The program uses Java NIO's WatchService for monitoring and AsynchronousFileChannel for logging.
 * Note: This implementation monitors only files directly in the specified directory, not subdirectories.
 */
public class RealTimeDirectoryMonitorAndAsynchronousLogger {
    private static final String logFileName = "log_file.txt";
    private static Path logFilePath = Paths.get(logFileName);

    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("Usage: java RealTimeDirectoryMonitorAndAsynchronousLogger <directory path>");
            return;
        }

        Path dir = Paths.get(args[0]);

        if(!Files.exists(dir)){
            System.out.println("Directory does not exist.");
            return;
        }

        if(!Files.isDirectory(dir)){
            System.out.println("Provided path is not a directory");
            return;
        }

        System.out.println("Note: This program monitors only files directly in the specified directory, not subdirectories.");

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            log("Starting to monitor the provided directory: " + dir.toAbsolutePath());
            WatchKey key = dir.register(watcher, 
                                        StandardWatchEventKinds.ENTRY_CREATE, 
                                        StandardWatchEventKinds.ENTRY_MODIFY, 
                                        StandardWatchEventKinds.ENTRY_DELETE);
            log("Now monitoring creation, modification, and deletion events.");

            while(true){
                WatchKey eventKey;

                try {
                    // Block until a key is available
                    eventKey = watcher.take();
                } catch (InterruptedException ex) {
                    System.out.println("Directory monitoring interrupted: " + ex.getMessage());
                    return;
                }
                
                // Process the events for the retrieved key
                for(WatchEvent<?> event : eventKey.pollEvents()){
                    WatchEvent.Kind<?> kind = event.kind();

                    // This is important to handle a key being invalid
                    if(kind == StandardWatchEventKinds.OVERFLOW){
                        continue;
                    }

                    // Get the file name associated with the event
                    // Suppress warnings for unchecked casts
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    log("Event kind: " + kind + ". File affected: " + fileName);
                }

                 // Reset the key to continue receiving events
                boolean valid = eventKey.reset();
                if(!valid){
                    // The key is no longer valid, exit the loop
                    break;
                }
            }

        } catch (IOException ex) {
            System.out.println("Error occured while setting up directory monitoring: " + ex.getMessage());
        }
    }

     /**
     * Logs a message to the log file.
     *
     * @param msg the message to log
     */
    private static void log(String msg){

        String logEntry = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " - " + msg + System.lineSeparator();
        ByteBuffer buffer = ByteBuffer.wrap(logEntry.getBytes());
        System.out.println("Attempting asynchronous write to file");
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(logFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            channel.write(buffer, channel.size(), null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer result, Void attachment) {
                    System.out.println("Asynchronous write completed successfully");
                    System.out.println(logEntry);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.out.println("Asynchronous write failed: " + exc.getMessage());
                }
            });
            // Wait for the asynchronous write to complete
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Error occurred while opening channel: " + e.getMessage());
        }
    }

}
