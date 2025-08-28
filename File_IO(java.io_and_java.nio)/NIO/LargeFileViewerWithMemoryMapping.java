import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class LargeFileViewerWithMemoryMapping {
    public static void main (String[] args){
        final long PAGE_SIZE = 4096; // Number of characters per page
        if (args.length != 2){
            System.out.println("Usage: java LargeFileViewerWithMemoryMapping <file path> <page number>");
            return;
        } else {
            String filePath = args[0];
            int pageNumber;
            try {
                pageNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid page number. Please provide a valid integer.");
                return;
            }
            long start = (pageNumber - 1) * PAGE_SIZE;
            long end = start + PAGE_SIZE;
            File file = new File(filePath);
            if(!file.exists() || (file.exists() && !file.getName().endsWith(".txt"))){
                System.out.println("File does not exist or is not a text file.");
            } else {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");) {
                    FileChannel channel = randomAccessFile.getChannel();
                    if (isValidPage(start, randomAccessFile.length())){
                        if(end <= channel.size()){
                            System.out.println("Displaying page " + pageNumber + " from file: " + filePath);
                            System.out.println();
                            MappedByteBuffer mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, start, end);
                            while(mappedBuffer.hasRemaining()){
                                System.out.print((char) mappedBuffer.get());
                            }
                            System.out.println();
                            System.out.println("--- End of page content ---");
                        }
                        if(end > channel.size()){
                            System.out.println("Displaying page " + pageNumber + " from file: " + filePath);
                            System.out.println();
                            MappedByteBuffer mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, start, channel.size());
                            while(mappedBuffer.hasRemaining()){
                                System.out.print((char) mappedBuffer.get());
                            }
                            System.out.println();
                            System.out.println("--- End of page content ---");
                        }
                    } else {
                        System.out.println("Invalid page number. Please provide a valid page number.");
                    }
                    channel.close();
                } catch (Exception ex) {
                    System.out.println("An error occured while trying to access the file and the required page: " + ex.getMessage());
                }

            }
        }
    }

    public static boolean isValidPage(long start, long fileSize){
        return start >= 0 && start <= fileSize;
    }

}
