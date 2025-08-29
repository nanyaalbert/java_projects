import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;

public class DirectoryFileFinderAndTruncator {
    public static void main (String[] args){
        if(args.length != 2){
            System.out.println("Usage: java DirectoryFileFinderAndTruncator <directory_path> <file_extension>");
            return;
        } else {
            Path directoryPath = Paths.get(args[0]);
            String fileExtension = args[1];

            if(!Files.exists(directoryPath)){
                System.out.println("Directory does not exist.");
                return;
            }
            if(!Files.isDirectory(directoryPath)){
                System.out.println("Provided path is not a directory.");
                return;
            }
            if(!fileExtension.startsWith(".")){
                System.out.println("Invalid file extension.");
                return;
            }
            if(Files.exists(directoryPath) && Files.isDirectory(directoryPath) && fileExtension.startsWith(".")){
                BiPredicate<Path, BasicFileAttributes> fileMatcher = (path, attrs) -> 
                    attrs.isRegularFile() && path.getFileName().toString().endsWith(fileExtension) && attrs.size() > 0;
                try {
                    Files.find(directoryPath, Integer.MAX_VALUE, fileMatcher)
                        .forEach(path -> {
                            try (FileChannel currentChannel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                                currentChannel.truncate(0L);
                                System.out.println("Truncated file: " + path.getFileName());
                            } catch (Exception ex) {
                                System.out.println("Error occurred while truncating file " + path.getFileName() + ": " + ex.getMessage());
                                ex.printStackTrace();
                                return;
                            }
                        });
                } catch (Exception ex) {
                    System.out.println("Error occured while searching for files: " + ex.getMessage());
                    return;
                }
            }
        }
    }

}
