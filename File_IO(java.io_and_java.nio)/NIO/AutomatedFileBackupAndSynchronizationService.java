import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutomatedFileBackupAndSynchronizationService {
    private static final String logFileName = "automated_file_backup_log.txt";
    private static Path logFilePath = Paths.get(logFileName);
    private static Path sourceDir;
    private static Path backupDir;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java AutomatedFileBackupAndSynchronizationService <source_directory_path>");
            return;
        }

        sourceDir = Paths.get(args[0]);
        if (!Files.exists(sourceDir)) {
            System.out.println("Source directory does not exist: " + sourceDir.toAbsolutePath());
            return;
        }

        if (!Files.isDirectory(sourceDir)) {
            System.out.println("Source path is not a directory: " + sourceDir.toAbsolutePath());
            return;
        }

        System.out.println("This program only backs up files in the specified directory, not its subdirectories.");
        System.out.println(
                "Backup directory will be created as a sibling to the source directory with '_backup' suffix.");
        System.out.println();

        setupBackDir(sourceDir);

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            sourceDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            log("Directory monitoring set up for: " + sourceDir.toAbsolutePath());

            while (true) {
                WatchKey key;

                try {
                    // Block until a key is available
                    key = watcher.take();
                } catch (InterruptedException e) {
                    log("Directory monitoring interrupted");
                    return;
                }

                // Process the events for the retrieved key
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // This is important to handle a key being invalid
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    // Get the file name associated with the event
                    // Suppress warnings for unchecked casts
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    // Perform the appropriate action based on the event kind
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        log("File created: " + filename);
                        backupFile(sourceDir.resolve(filename), backupDir.resolve(filename));
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        log("File modified: " + filename);
                        backupFile(sourceDir.resolve(filename), backupDir.resolve(filename));
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        log("File deleted: " + filename);
                        deleteBackupFile(backupDir.resolve(filename));
                    }
                }

                // Reset the key to continue receiving events
                boolean valid = key.reset();

                if (!valid) {
                    break; // The key is no longer valid, exit the loop
                }
            }

        } catch (IOException ex) {
            log("Error occured while setting up directory monitoring for: " + sourceDir.toAbsolutePath());
        }

    }

    public static void setupBackDir(Path sourceDir) {
        try {
            backupDir = sourceDir.resolveSibling(sourceDir.getFileName() + "_backup");
            if (!Files.exists(backupDir)) {
                log("Creating backup directory: " + backupDir.toAbsolutePath());
                Files.createDirectory(backupDir);
                log("Backup directory created: " + backupDir.toAbsolutePath());
                backupExistingFiles(sourceDir, backupDir);
            } else {
                log("Backup directory already exists: " + backupDir.toAbsolutePath());
                backupExistingFiles(sourceDir, backupDir);
            }
        } catch (IOException e) {
            log("Failed to create backup directory for: " + sourceDir.getFileName() + ". Error: " + e.getMessage());
        }
    }

    public static void backupExistingFiles(Path sourceDir, Path backupDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            log("Backing up existing files from: " + sourceDir.toAbsolutePath() + " to: " + backupDir.toAbsolutePath());
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    backupFile(entry, backupDir.resolve(entry.getFileName()));
                }
            }
            log("Finished backing up existing files.");
        } catch (Exception ex) {
            log("Failed to backup existing files. Error: " + ex.getMessage());
        }
    }

    private static void backupFile(Path source, Path destination) {
        try {
            if (Files.isRegularFile(source)) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                log("Backup created for file: " + source.getFileName() + " at " + destination.toAbsolutePath());
            }

        } catch (IOException e) {
            log("Failed to create backup for file: " + source.getFileName() + ". Error: " + e.getMessage());
        }
    }

    private static void deleteBackupFile(Path backupFile) {
        try {
            if (Files.isRegularFile(backupFile)) {
                Files.deleteIfExists(backupFile);
                log("Backup deleted for file: " + backupFile.getFileName());
            }
        } catch (IOException e) {
            log("Failed to delete backup for file: " + backupFile.getFileName() + ". Error: " + e.getMessage());
        }
    }

    /**
     * Logs a message to the log file.
     *
     * @param msg the message to log
     */
    private static void log(String msg) {

        String logEntry = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " - " + msg
                + System.lineSeparator();
        ByteBuffer buffer = ByteBuffer.wrap(logEntry.getBytes());
        System.out.println("Attempting asynchronous write to file");
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(logFilePath, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
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
