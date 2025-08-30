import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleFileEncryptionAndDecryption {
    private static ByteBuffer inputBuffer;
    private static ByteBuffer outputBuffer;

    public static void main(String[] args){
        if(args.length != 3){
            System.out.println("Usage: java SimpleFileEncryptionAndDecryption < encrypt | decrypt > <key> <input file>");
            return;
        }

        Path path = Paths.get(args[2]);
        byte key;
        try {
            key = Byte.parseByte(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("key should be a numeric value between -128 to 127");
            e.printStackTrace();
            return;
        }

        if(!Files.exists(path)){
            System.out.println("Input file does not exist.");
            return;
        }

        try {
            inputBuffer = ByteBuffer.allocate((int) Files.size(path));
            outputBuffer = ByteBuffer.allocate((int) Files.size(path));
            try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);) {
                if(args[0].equalsIgnoreCase("encrypt")){
                    encryptAndDecryptFile(fileChannel, key, path, "Encryption");
                } else if(args[0].equalsIgnoreCase("decrypt")){
                    encryptAndDecryptFile(fileChannel, key, path, "Decryption");
                } else {
                    System.out.println("First argument should be either 'encrypt' or 'decrypt'");
                }
                
            } catch (IOException e) {
                System.out.println("An error occurred while trying to open the file.");
                e.printStackTrace();
                return;
            }
        } catch (IOException e) {
            System.out.println("An error occured while trying to get the file size.");
            e.printStackTrace();
            return;
        }
    }

    private static void encryptAndDecryptFile(FileChannel fileChannel, byte key, Path inputFile, String mode){
        try {
            inputBuffer.clear();
            fileChannel.position(0).read(inputBuffer);
            inputBuffer.flip();
            outputBuffer.clear();
            while(inputBuffer.hasRemaining()){
                outputBuffer.put((byte)(inputBuffer.get() ^ key));
            }
            outputBuffer.flip();
            fileChannel.position(0).write(outputBuffer);
            fileChannel.force(true);
            fileChannel.close();
            System.out.println(mode + " of " + inputFile.getFileName() + " was successful.");
        } catch (IOException e) {
            System.out.println("An error occured while trying to read the file.");
            e.printStackTrace();
            return;
        }
    }
}
