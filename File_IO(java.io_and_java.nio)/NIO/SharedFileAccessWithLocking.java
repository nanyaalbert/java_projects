import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class SharedFileAccessWithLocking {

    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    private Random random = new Random();
    
    public static void main (String[] args){
        if (args.length != 2){
            System.out.println("Usage: java SharedFileAccessWithLocking < r | w > <file_path>");
            return;
        }
        if (!args[0].equals("r") || !args[0].equals("w")) {
            System.out.println("Invalid access mode. Use 'r' for read or 'w' for write.");
            return;
        }

        boolean writer = args[0].equals("w");
        Path path = Paths.get(args[1]);

        if(!Files.exists(path)){
            System.out.println("File does not exist.");
            return;
        }

        if(!Files.isRegularFile(path)){
            System.out.println("File is not a regular file.");
            return;
        }

        
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), writer ? "rw" : "r");) {
            SharedFileAccessWithLocking app = new SharedFileAccessWithLocking();
            FileChannel fileChannel = file.getChannel();
            if (writer) {
                app.writeFile(fileChannel);
            } else {
                app.readFile(fileChannel);
            }
        } catch (IOException ex) {
            System.out.println("An error occured while accessing the file: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

    }

    private void readFile(FileChannel fileChannel) {
        while(true){
            try {
                println("Attempting to acquire lock for reading...");
                FileLock lock = fileChannel.lock(0, fileChannel.size(), true);
                byteBuffer.clear();
                fileChannel.read(byteBuffer, 0);
                byteBuffer.flip();
                while(byteBuffer.hasRemaining()) {
                    println("Read integer: " + byteBuffer.getInt() + " from file");
                }
                println("Finished reading from the file");
                lock.release();
                try {
                    println("Sleeping for a while...");
                    Thread.sleep(random.nextInt(500) + 500);
                } catch (InterruptedException e) {
                    println("Reader Thread was interrupted");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("Error occurred while locking the file: " + e.getMessage());
            }
        }        
    }

    private void writeFile(FileChannel fileChannel) {
        while(true){
            try {
                println("Attempting to acquire lock for writing...");
                FileLock lock = fileChannel.lock(0, fileChannel.size(), false);
                int number = random.nextInt(100) + 100;
                byteBuffer.clear();
                for (int i = 0; i <= number; i++) {
                    int value = random.nextInt(100);
                    byteBuffer.putInt(value);
                    println("Writing the integer " + value + " to the file");
                }
                byteBuffer.flip();
                fileChannel.write(byteBuffer, 0);
                fileChannel.force(true);
                println("Finished writing to the file");
                lock.release();
                try {
                    println("Sleeping for a while...");
                    Thread.sleep(random.nextInt(500) + 500);
                } catch (InterruptedException e) {
                    println("Writer Thread was interrupted");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("Error occurred while locking the file: " + e.getMessage());
            }
        }
    
    }

    private int lastLineLength = 0;

    private void println(String message){
        System.out.print("\r " + message);
        int currentLineLength = "\r ".length() + message.length();
        for (int i = currentLineLength; i < lastLineLength; i++) {
            System.out.print(" ");
        }
        lastLineLength = currentLineLength;
    }

}

