import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleSecureFileSharingServerWithEncryption {
    private static final int PORT = 1234;
    private static final int TEXT = 0;
    private static final int FILE_SEND_REQUEST = 1;
    private static final int FILE_UPLOAD_REQUEST = 2;
    private static final int FILE_DOWNLOAD_REQUEST = 3;
    private static final int FILE_LIST_REQUEST = 4;
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;
    private static final long MAX_ENCRYPTED_FILE_SIZE = MAX_FILE_SIZE + 64; // 64 bytes safety margin for encryption
    private static final int MAX_FILE_NAME_LENGTH = 512;
    private static final int MAX_ENCRYPTED_FILE_NAME_LENGTH = MAX_FILE_NAME_LENGTH + 64; // 64 bytes safety margin for encryption
    private static final String HANDSHAKE = "SimpleSecureFileSharingHandshake";
    private static Path downloadPath = Paths.get(System.getProperty("user.home"), "SimpleSecureFileSharingServerWithEncryption");
    private static ByteBuffer sendBuffer = ByteBuffer.allocate(1024 * 100);
    private static ByteBuffer receiveBuffer = ByteBuffer.allocate(1024 * 100);
    private static ByteBuffer fileNameBuffer = ByteBuffer.allocate(MAX_ENCRYPTED_FILE_NAME_LENGTH);
    private static ByteBuffer commandBuffer = ByteBuffer.allocate(4);
    private static ByteBuffer fileNameLengthBuffer = ByteBuffer.allocate(4);

    public static void main(String[] args){
        System.out.println("Welcome");
        if(Files.notExists(downloadPath)){
            try{
                Files.createDirectories(downloadPath);
            }catch(IOException e){
                System.err.println("An error occured when creating the download directory: " + e.getMessage());
                return;
            }
        }        

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open(); Selector selector = Selector.open()) {
            serverChannel.configureBlocking(false);
            System.out.println("Waiting for connections...");
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", PORT);
            serverChannel.bind(serverAddress);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(true){
                if(selector.select() == 0) continue;

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if(!key.isValid()) continue;

                    if(key.isAcceptable()){
                        ServerSocketChannel readyServer = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = readyServer.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println("Client " + clientChannel.getRemoteAddress() + " Connected");
                        Object lockObj = readyServer.blockingLock();
                        synchronized(lockObj){
                            boolean prevState = readyServer.isBlocking();
                            readyServer.configureBlocking(true);
                            if(!performHandshakeWithClient(clientChannel)){
                                clientChannel.close();
                            }
                            readyServer.configureBlocking(prevState);
                        }                 
                    }

                    if(key.isReadable()){
                        SocketChannel readyClient = (SocketChannel) key.channel();
                        ByteBuffer[] details = {commandBuffer, fileNameLengthBuffer};
                        readyClient.read(details);
                        int command = commandBuffer.getInt();
                        int fileNameLength = fileNameLengthBuffer.getInt();
                        fileNameBuffer.clear().limit(fileNameLength);
                        readyClient.read(fileNameBuffer);
                        String fileName = new String(fileNameBuffer.flip().array(), StandardCharsets.UTF_8);
                        if(command == FILE_SEND_REQUEST){
                            
                        }else if(command == FILE_UPLOAD_REQUEST){

                        }else if(command == FILE_LIST_REQUEST){

                        }else{
                            System.err.println("Client " + readyClient.getRemoteAddress() + " sent an invalid command");
                        }
                        
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("An error occured with the server: " + e.getMessage());
        }
        
    }

    public static boolean performHandshakeWithClient(SocketChannel clientChannel){
        try {
            int bytesWritten = clientChannel.write(sendBuffer.clear().put(HANDSHAKE.getBytes(StandardCharsets.UTF_8)).flip());
            if(bytesWritten < 0){
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection, handshake failed");
                return false;
            } else if(bytesWritten > 0){
                int bytesRead = clientChannel.read(receiveBuffer.clear());
                if(bytesRead < 0){
                    System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection, handshake failed");
                    return false;
                } else if(bytesRead > 0){
                    if(StandardCharsets.UTF_8.decode(receiveBuffer.flip()).toString().equals(HANDSHAKE)) return true;
                    else {
                        System.err.println("Client " + clientChannel.getRemoteAddress() + " is not a valid SimplesecureFileSharingClient");
                        return false;
                    }
                }
            }
            return false;            
        } catch (IOException e) {
            System.err.println("An error occured during handshake with client" + e.getMessage());
            return false;
        }
    }

    public static long filesList(SocketChannel clientChannel){
        long bytesWritten;
        String filesListString;
        commandBuffer.clear().putInt(FILE_LIST_REQUEST).flip();
        fileNameBuffer.clear().put("Files".getBytes(StandardCharsets.UTF_8));
        fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();;
        try (Stream<Path> files = Files.list(downloadPath)) {
            if(files.filter(Files::isRegularFile).findAny().isPresent()){
                filesListString = files.filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).collect(Collectors.joining("\n"));
                sendBuffer.clear().put(filesListString.getBytes(StandardCharsets.UTF_8)).flip();
                ByteBuffer[] buffers = {commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer};
                try {
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                    System.err.println("An error occured while trying to send file list to " + clientChannel.getRemoteAddress());
                }           
            }
            filesListString = "No files available on server";
            sendBuffer.clear().put(filesListString.getBytes(StandardCharsets.UTF_8)).flip();
            ByteBuffer[] buffers = {commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer};
            try {
                bytesWritten = clientChannel.write(buffers);
                return bytesWritten;
            } catch (IOException e) {
                System.err.println("An error occured while trying to send file list to " + clientChannel.getRemoteAddress());
            }
        } catch (Exception e) {
            System.err.println("An error occured while trying to get file list " + e.getMessage());
        }
        return 0L;
    }

    public static long sendFile(String fileName, SocketChannel clientChannel){
        Path fileToSend = downloadPath.resolve(fileName);
        long bytesWritten;
        try {
            if(Files.notExists(fileToSend)){
            String information = fileName + " does not exist.";
            commandBuffer.clear().putInt(TEXT).flip();
            fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
            fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
            sendBuffer.clear().put(information.getBytes(StandardCharsets.UTF_8)).flip();
            ByteBuffer[] buffers = {commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer};
            try {
                bytesWritten = clientChannel.write(buffers);
                return bytesWritten;
            } catch (IOException e) {
               System.err.println("An error occured while trying to send information to " + clientChannel.getRemoteAddress());
            }
            }else if(Files.exists(fileToSend)){
                try (FileChannel fileChannel = FileChannel.open(fileToSend, StandardOpenOption.READ)) {
                    commandBuffer.clear().putInt(FILE_DOWNLOAD_REQUEST).flip();
                    fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
                    fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
                    MappedByteBuffer fileDataBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                    ByteBuffer[] buffers = {commandBuffer, fileNameLengthBuffer, fileNameBuffer, fileDataBuffer};
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                    System.err.println("An error occured while trying to send file " + fileName +  " to " + clientChannel.getRemoteAddress());
                }                
            }else{
                String information = "Server could not send file" + fileName;
                commandBuffer.clear().putInt(TEXT).flip();
                fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
                fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
                sendBuffer.clear().put(information.getBytes(StandardCharsets.UTF_8)).flip();
                ByteBuffer[] buffers = {commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer};
                try {
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                System.err.println("An error occured while trying to send information to " + clientChannel.getRemoteAddress());
                }
            }
        } catch (IOException e) {             
        }
        return bytesWritten = 0L;        
    }

    public static void saveFile(String fileName, ByteBuffer fileDataBuffer){
        Path filePath = downloadPath.resolve(fileName);
        int lastDotIndex = fileName.lastIndexOf(".");
        String fileExtension = fileName.substring(lastDotIndex);
        String fileNameWithoutExtension = fileName.substring(0 , lastDotIndex);
        int counter = 0;
        while(true){
            if(Files.exists(filePath)){
                counter++;
                filePath = downloadPath.resolve(fileNameWithoutExtension + "(" + counter + ")" + fileExtension);
            }else break;
        }
        
        try (AsynchronousFileChannel asyncfileChannel = AsynchronousFileChannel.open(filePath,StandardOpenOption.WRITE)) {
            asyncfileChannel.write(fileDataBuffer, 0, null, new CompletionHandler<Integer, Void>(){
                @Override
                public void completed(Integer result, Void attachment) {
                    System.out.println(fileName + " was successfully saved");
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.out.println(fileName + " could not be saved");
                }
            });
        } catch (Exception e) {
            System.err.println("An error occured while saving " + fileName + " to file.");
        }
    }

}
