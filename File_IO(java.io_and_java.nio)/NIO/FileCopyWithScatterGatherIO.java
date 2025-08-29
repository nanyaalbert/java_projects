import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileCopyWithScatterGatherIO {
    public static void main (String[] args){
        if(args.length != 2){
            System.out.println("Usage: java FileCopyWithScatterGatherIO <source_file> <destination_file>");
            return;
        }

        Path sourceFilePath = Paths.get(args[0]);
        Path destinationFilePath = Paths.get(args[1]);

        if(!Files.exists(sourceFilePath)){
            System.out.println("Source file does not exist.");
            return;
        }
        if(!Files.exists(destinationFilePath)){
            System.out.println("Destination file does not exist.");
            return;
        }

        try {
            long size = Files.size(sourceFilePath);
            ByteBuffer header = ByteBuffer.allocate(12);
            ByteBuffer body = ByteBuffer.allocate((int) size - 12);
            ByteBuffer[] bufferArray = {header, body};
            try (RandomAccessFile sourceRaf = new RandomAccessFile(sourceFilePath.toFile(), "r"); RandomAccessFile destRaf = new RandomAccessFile(destinationFilePath.toFile(), "rw")) {
                FileChannel sourceChannel = sourceRaf.getChannel();
                FileChannel destChannel = destRaf.getChannel();
                System.out.println("Scattering file content to bufferArray ...");
                sourceChannel.read(bufferArray);
                System.out.println("Scatter was successful");
                System.out.println();
                System.out.println("Gathering file content from bufferArray ...");
                for (ByteBuffer buffer : bufferArray) {
                    buffer.flip();
                }
                destChannel.write(bufferArray);
                System.out.println("Gather was successful");
                sourceChannel.close();
                destChannel.close();
            } catch (IOException ex) {
                System.err.println("Error occurred during file copy: " + ex.getMessage());
                return;
            }
        } catch (NoSuchFileException ex) {
            System.err.println("Error: The file does not exist: " + ex.getMessage());
            return;
        } catch (IOException ex) {
            System.err.println("An I/O error occurred: " + ex.getMessage());
            return;
        }
    }

}
