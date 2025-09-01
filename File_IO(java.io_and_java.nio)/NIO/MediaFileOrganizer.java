import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MediaFileOrganizer is a utility class for organizing media files
 * (images, videos, audios) into a structured directory format.
 * It provides functionality to scan a directory, process media files,
 * and move them into appropriate subdirectories based on their type
 * and creation date.
 * It generates a log file to track the organization process.
 * it also deletes empty directories after processing.
 */

public class MediaFileOrganizer {
    private static final String logFileName = "media_organizer_log.txt";
    private static Path logFilePath;
    private static Path mediaDir;
    private static Path imagesDir;
    private static Path videosDir;
    private static Path audiosDir;

    public static void main(String[] args){
                
        if(args.length != 1){
            System.out.println("Usage: java MediaFileOrganizer <directory path>");
            return;
        }

        logFilePath = Paths.get(args[0], logFileName);

        try {
            if(!Files.exists(logFilePath)){
                Files.createFile(logFilePath);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if(Files.isDirectory(Paths.get(args[0])) && Files.exists(Paths.get(args[0])) && Files.isReadable(Paths.get(args[0])) && Files.isWritable(Paths.get(args[0]))){
            mediaDir = createNewDirectory(Paths.get(args[0], "Media"));
            imagesDir = createNewDirectory(Paths.get(mediaDir.toString(), "Images"));
            videosDir = createNewDirectory(Paths.get(mediaDir.toString(), "Videos"));
            audiosDir = createNewDirectory(Paths.get(mediaDir.toString(), "Audio"));

            log("Starting to scan directory: " + args[0]);
            scanDirectory(Paths.get(args[0]));
            log("Finished scanning directory: " + args[0]);
        } else log("The provided path is not a valid directory or lacks necessary permissions " + args[0]);

        

    }

    /**
     * Scans a directory for media files and organizes them into subdirectories.
     * It deletes empty directories after processing.
     *
     * @param path the path to the directory
     */
    private static void scanDirectory(Path path){
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !entry.equals(mediaDir)) {
                    log("Entering directory: " + entry);
                    scanDirectory(entry);
                    log("Scanned directory: " + entry);
                    try {
                        log("Attempting to delete directory: " + entry);
                        Files.delete(entry);
                        log("Deleted empty directory: " + entry);
                    } catch (IOException ex) {
                        log("Could not delete directory (not empty or error): " + entry + " " + ex.getMessage());
                    }
                } else {
                    log("Found file: " + entry);
                    processFile(entry);
                }
            }
        } catch (Exception ex) {
            log("Error scanning directory: " + path + " "  + ex.getMessage());
        }
    }

    /**
     * Processes a single file, determining its type and moving it to the appropriate directory.
     *
     * @param path the path to the media file
     */
    private static void processFile(Path path){
        if(!path.getFileName().toString().equals(logFileName)){
            switch(getFileExtension(path)){
                case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".tiff", ".svg" -> {
                    ZonedDateTime creationTime = extractCreationTime(path);
                    if(creationTime != null){
                        Path targetDir = createNewDirectory(Paths.get(imagesDir.toAbsolutePath().toString(), String.valueOf(creationTime.getYear()), String.format("%02d", creationTime.getMonthValue())));
                        moveFileToDirectory(path, targetDir);
                        log("Moved image file: " + path.getFileName() + " to " + targetDir);
                    }
                }
                case ".mp4", ".mov", ".mkv", ".avi" -> {
                    ZonedDateTime creationTime = extractCreationTime(path);
                    if(creationTime != null){
                        Path targetDir = createNewDirectory(Paths.get(videosDir.toAbsolutePath().toString(), String.valueOf(creationTime.getYear()), String.format("%02d", creationTime.getMonthValue())));
                        moveFileToDirectory(path, targetDir);
                        log("Moved video file: " + path.getFileName() + " to " + targetDir);
                    }
                }
                case ".mp3", ".wav" -> {
                    ZonedDateTime creationTime = extractCreationTime(path);
                    if(creationTime != null){
                        Path targetDir = createNewDirectory(Paths.get(audiosDir.toAbsolutePath().toString(), String.valueOf(creationTime.getYear()), String.format("%02d", creationTime.getMonthValue())));
                        moveFileToDirectory(path, targetDir);
                        log("Moved audio file: " + path.getFileName() + " to " + targetDir);
                    }
                }
                default -> log("Unsupported file type: " + path);
            }
        }
        
    }

    /**
     * Gets the file extension of a given file path.
     *
     * @param path the file path
     * @return the file extension, or an empty string if none exists
     */
    private static String getFileExtension(Path path){
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf(".");
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex).toLowerCase();
    }

    private static Path createNewDirectory(Path path){
        try {
            if(!Files.exists(path)){
                Files.createDirectories(path);
                log("Created new directory: " + path);
                return path;
            }
            return path;
        } catch (IOException ex) {
            log("Error creating directory: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Extracts the creation time of a media file.
     *
     * @param filePath the path to the media file
     * @return the creation time as a ZonedDateTime, or null if not available
     */
    private static ZonedDateTime extractCreationTime(Path filePath) {
        try {
            return Files.readAttributes(filePath, BasicFileAttributes.class).creationTime().toInstant().atZone(ZoneId.systemDefault());
        } catch (IOException ex) {
            log("");
            return null;
        }
    }

    /**
     * Moves a file to a specified directory.
     *
     * @param sourceFile the file to move
     * @param targetDirectory the directory to move the file to
     */
    private static void moveFileToDirectory(Path sourceFile, Path targetDirectory){
        try {
            Path destDirectory;
            if(!Files.exists(targetDirectory)){
                destDirectory = createNewDirectory(targetDirectory);
            }else destDirectory = targetDirectory;
            if(!destDirectory.equals(null)){
                Path destFilePath = destDirectory.resolve(sourceFile.getFileName());
                if(Files.exists(destFilePath)){
                    Path newDestFilePath = handleDuplicates(destFilePath);
                    log("Duplicate found. Renaming to: " + newDestFilePath.getFileName());
                    Files.move(sourceFile, newDestFilePath);
                    return;
                }
                Files.move(sourceFile, destFilePath);
            } else log("Error: Destination directory is null");

        } catch (IOException ex) {
            log("Error moving file: " + ex.getMessage());
        }
    }

    /**
     * Handles duplicate files by renaming them.
     *
     * @param path the path to the file
     * @return the new path for the file
     */
    private static Path handleDuplicates(Path path){
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf(".");
        String baseName = (lastDotIndex == -1) ? fileName: fileName.substring(0, lastDotIndex);
        String extension = (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex);
        int counter = 1;

        String newFileName = baseName + " (" + counter + ")" + extension;

        while(Files.exists(path.resolveSibling(newFileName))){
            counter++;
            newFileName = baseName + " (" + counter + ")" + extension;
        }
        return path.resolveSibling(newFileName);
    }

    /**
     * Logs a message to the log file.
     *
     * @param msg the message to log
     */
    private static void log(String msg){
        try {
            String logEntry = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " - " + msg + System.lineSeparator();
            Files.writeString(logFilePath, logEntry, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

