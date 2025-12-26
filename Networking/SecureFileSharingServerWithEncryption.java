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

public class SecureFileSharingServerWithEncryption {
    private static final int PORT = 1234;
    private static final int INFORMATION = 0;
    private static final int FILE_SEND_REQUEST = 1;
    private static final int FILE_UPLOAD_REQUEST = 2;
    private static final int FILE_DOWNLOAD = 3;
    private static final int FILE_LIST_REQUEST = 4;
    private static final int FILE_LIST = 5;
    private static final int HANDSHAKE = 6;
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;
    private static final long MAX_ENCRYPTED_FILE_SIZE = MAX_FILE_SIZE + 64; // 64 bytes safety margin
    private static final int MAX_FILE_NAME_LENGTH = 512;
    private static final int MAX_ENCRYPTED_FILE_NAME_LENGTH = MAX_FILE_NAME_LENGTH + 64; // 64 bytes safety margin
    private static final String HANDSHAKE_STRING = "SecureFileSharingHandshake";
    private static long TEMPFILENUMBER = 0;
    private static Path serverDownloadPath = Paths.get(System.getProperty("user.home"),
            "SecureFileSharingServerWithEncryption");
    private static Path serverTempPath = Paths.get(System.getProperty("user.home"),
            "SecureFileSharingServerWithEncryptionTemp");

    private enum Progress {
        JUST_CONNECTED,
        READING_FILENAMELENGTH,
        READING_FILESIZE,
        READING_FILENAME,
        READING_FILEDATA,
        READING_INFORMATION,
        READING_HANDSHAKE,
        READING_FILELIST,
        WRITING_COMMAND,
        WRITING_FILENAMElENGTH,
        WRITING_FILESIZE,
        WRITING_FILENAME,
        WRITING_FILEDATA,
        WRITING_INFORMATION,
        WRITING_HANDSHAKE,
        WRITING_FILELIST,
        VALID_HANDSHAKE,
        FILE_LIST_SAVED_TO_DISK
    }

    public static void main(String[] args) {
    }

    private static void server() {
        System.out.println("Welcome to the server");
        if (Files.notExists(serverDownloadPath)) {
            try {
                Files.createDirectories(serverDownloadPath);
            } catch (IOException e) {
                System.err.println("An error occured when creating the download directory: " + e.getMessage());
                return;
            }
        }

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open(); Selector selector = Selector.open()) {
            serverChannel.configureBlocking(false);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", PORT);
            serverChannel.bind(serverAddress);
            System.out.println("Server listening on port " + PORT);
            System.out.println("Waiting for connections...");
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                if (selector.select() == 0)
                    continue;

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid())
                        continue;

                    // ByteBuffer handShakeBuffer = ByteBuffer.allocate(1024);
                    // ByteBuffer sendBuffer = ByteBuffer.allocate(1024 * 1024);
                    // ByteBuffer receiveBuffer = ByteBuffer.allocate(1024 * 1024);
                    // ByteBuffer commandBuffer = ByteBuffer.allocate(4);
                    // ByteBuffer fileNameLengthBuffer = ByteBuffer.allocate(4);
                    // ByteBuffer handShakeLength = ByteBuffer.allocate(4);
                    // ByteBuffer fileSizeBuffer = ByteBuffer.allocate(8);

                    if (key.isAcceptable()) {
                        ServerSocketChannel readyServer = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = readyServer.accept();
                        clientChannel.configureBlocking(false);
                        CurrentSession currentSession = new CurrentSession();
                        currentSession.progressState = Progress.JUST_CONNECTED;
                        currentSession.handShakeSendBuffer = ByteBuffer
                                .wrap(HANDSHAKE_STRING.getBytes(StandardCharsets.UTF_8));
                        currentSession.handShakeSendLengthBuffer.putInt(currentSession.handShakeSendBuffer.capacity());
                        clientChannel.register(selector, SelectionKey.OP_READ, currentSession);
                        System.out.println("Client " + clientChannel.getRemoteAddress() + " just connected");
                    }

                    if (key.isReadable()) {
                        SocketChannel readyClient = (SocketChannel) key.channel();
                        CurrentSession keySession = (CurrentSession) key.attachment();
                        readyClient.read(keySession.commandBuffer);
                        int command;
                        if (keySession.commandBuffer.remaining() >= 4) {
                            command = keySession.commandBuffer.getInt();
                        } else
                            continue;

                        switch (keySession.progressState) {
                            case Progress.JUST_CONNECTED -> {
                                writeHandshake(key);
                                readHandshake(key);
                            }
                            case Progress.READING_HANDSHAKE -> {
                                readHandshake(key);
                            }
                            case Progress.VALID_HANDSHAKE -> {
                                if (command == FILE_SEND_REQUEST) {
                                }
                                if (command == FILE_UPLOAD_REQUEST) {
                                }
                                if (command == FILE_LIST_REQUEST) {
                                }

                            }

                        }

                        if (command == FILE_SEND_REQUEST) {
                            ByteBuffer[] details = { fileNameLengthBuffer, fileSizeBuffer };
                            readyClient.read(details);
                            int fileNameLength = fileNameLengthBuffer.getInt();
                            long fileSize = fileSizeBuffer.getLong();
                            ByteBuffer fileNameBuffer = ByteBuffer.allocate(fileNameLength);
                            readyClient.read(fileNameBuffer);
                            String fileName = new String(fileNameBuffer.flip().array(), StandardCharsets.UTF_8);
                            ByteBuffer[] buffersArr = { commandBuffer, fileNameLengthBuffer, fileSizeBuffer,
                                    fileNameBuffer, sendBuffer };
                            long bytesWritten = serverSendFile(fileName, readyClient, buffersArr);
                            if (bytesWritten < 0) {
                                System.err.println("Client " + readyClient.getRemoteAddress()
                                        + " closed the connection, file send failed");
                                cancelKey(key);
                            } else if (bytesWritten > 0) {
                                System.out.println(
                                        fileName + " was successfully sent to " + readyClient.getRemoteAddress());
                            } else {
                                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                key.attach(buffersArr); // change this to the inline class(I'll define the class later)
                            }
                        } else if (command == FILE_UPLOAD_REQUEST) {
                            readyClient.read(fileNameLengthBuffer.clear());
                            int fileNameLength = fileNameLengthBuffer.getInt();
                            ByteBuffer fileNameBuffer = ByteBuffer.allocate(fileNameLength);
                            int bytesRead = 0;
                            while (bytesRead < fileNameLength) {
                                bytesRead += readyClient.read(fileNameBuffer);
                            }
                            String fileName = new String(fileNameBuffer.flip().array(), StandardCharsets.UTF_8);

                        } else if (command == FILE_LIST_REQUEST) {

                        } else {
                            System.err.println("Client " + readyClient.getRemoteAddress() + " sent an invalid command");
                        }

                    }
                }
            }

        } catch (Exception e) {
            System.out.println("An error occured with the server: " + e.getMessage());
        }

    }

    private static void readHandshake(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        CurrentSession keySession = (CurrentSession) key.attachment();
        int handShakeLength;
        try {
            if (keySession.handShakeReceiveLengthBuffer.remaining() != keySession.handShakeReceiveLengthBuffer
                    .capacity()) {
                int bytesRead;
                bytesRead = clientChannel.read(keySession.handShakeReceiveLengthBuffer);
                if (bytesRead < 0) {
                    System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                    cancelKey(key);
                } else if (bytesRead > 0 && keySession.handShakeReceiveLengthBuffer.remaining() == 0) {
                    keySession.progressState = Progress.READING_HANDSHAKE;
                    keySession.handShakeReceiveBuffer = ByteBuffer
                            .allocate(keySession.handShakeReceiveLengthBuffer.getInt());
                }
            }

            if (keySession.progressState == Progress.READING_HANDSHAKE) {
                int bytesRead;
                handShakeLength = keySession.handShakeReceiveLengthBuffer.getInt();
                if (keySession.handShakeReceiveBuffer.remaining() != handShakeLength) {
                    bytesRead = clientChannel.read(keySession.handShakeReceiveBuffer);
                    if (bytesRead < 0) {
                        System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                        cancelKey(key);
                    } else if (bytesRead > 0 && keySession.handShakeReceiveBuffer.position() == handShakeLength) {
                        keySession.handShakeReceiveBuffer.flip();
                        String receivedHandShake = StandardCharsets.UTF_8.decode(keySession.handShakeReceiveBuffer)
                                .toString();
                        if (receivedHandShake.equals(HANDSHAKE_STRING)) {
                            keySession.progressState = Progress.VALID_HANDSHAKE;
                            keySession.handShakeReceiveBuffer.clear();
                            keySession.handShakeReceiveLengthBuffer.clear();
                        } else
                            cancelKey(key);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("An error occured with the client " + clientChannel.getRemoteAddress());
        }

    }

    private static void writeHandshake(SelectionKey key) throws IOException {
        long bytesWritten;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        CurrentSession keySession = (CurrentSession) key.attachment();
        try {
            ByteBuffer[] handshakeBufferArrs = { keySession.handShakeSendLengthBuffer, keySession.handShakeSendBuffer };
            bytesWritten = clientChannel.write(handshakeBufferArrs);
            if (bytesWritten < 0) {
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                cancelKey(key);
            } else if (bytesWritten > 0 && !keySession.handShakeSendBuffer.hasRemaining()) {
                keySession.handShakeSendLengthBuffer.clear();
                keySession.handShakeSendBuffer.clear();
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            } else if (bytesWritten == 0) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            System.err.println("An error occured with the client " + clientChannel.getRemoteAddress());
        }

    }

    private static void serverSendFilesList(SelectionKey key) throws IOException {
        CurrentSession keySession = (CurrentSession) key.attachment();
        SocketChannel clientChannel = (SocketChannel) key.channel();
        if (keySession.fileChannel == null || !keySession.fileChannel.isOpen()) {
            if (keySession.progressState != Progress.FILE_LIST_SAVED_TO_DISK) {
                keySession.fileListTempFile = serverTempPath.resolve("temp_file_list" + TEMPFILENUMBER++);
                AsynchronousFileChannel asyncFileChannel = AsynchronousFileChannel.open(
                        keySession.fileListTempFile, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE);
                try (Stream<Path> files = Files.list(serverDownloadPath)) {
                    String fileList;
                    if (files.filter(Files::isRegularFile).findAny().isPresent()) {
                        fileList = files.filter(Files::isRegularFile).map(Path::getFileName)
                                .map(Path::toString)
                                .collect(Collectors.joining("\n"));
                    } else
                        fileList = "No files available on server";
                    keySession.fileListStringLength = fileList.length();
                    int prevOps = key.interestOps();
                    key.interestOps(0);
                    asyncFileChannel.write(ByteBuffer.wrap(fileList.getBytes(StandardCharsets.UTF_8)), 0, key,
                            new CompletionHandler<Integer, SelectionKey>() {
                                @Override
                                public void completed(Integer result, SelectionKey attachment) {
                                    attachment.interestOps(prevOps);
                                    CurrentSession keySession = (CurrentSession) attachment.attachment();
                                    try {
                                        keySession.fileChannel = FileChannel.open(keySession.fileListTempFile,
                                                StandardOpenOption.READ);
                                        keySession.progressState = Progress.FILE_LIST_SAVED_TO_DISK;
                                    } catch (Exception e) {
                                        System.out.println("An error occured while sending file list to client ");
                                    }
                                }

                                @Override
                                public void failed(Throwable exc, SelectionKey attachment) {
                                    attachment.interestOps(prevOps);
                                    System.err.println(
                                            "An error occured while writing fileList to file: " + exc.getMessage());
                                }
                            });

                } catch (Exception e) {
                    System.err.println("An error occured while trying to get file list " + e.getMessage());
                }
            }
        } else if (keySession.progressState == Progress.FILE_LIST_SAVED_TO_DISK) {
            if (keySession.fileChannel == null || !keySession.fileChannel.isOpen())
                return;
            long bytesWritten;
            keySession.commandBuffer.putInt(FILE_LIST);
            keySession.fileListLengthBuffer.putInt(keySession.fileListStringLength);
            ByteBuffer[] bufferArr = { keySession.commandBuffer, keySession.fileListLengthBuffer };
            bytesWritten = clientChannel.write(bufferArr);
            if (bytesWritten < 0) {
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                cancelKey(key);
            } else if (bytesWritten > 0 && !keySession.fileListLengthBuffer.hasRemaining()) {
                keySession.commandBuffer.clear();
                keySession.fileListLengthBuffer.clear();
                keySession.progressState = Progress.WRITING_FILELIST;
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            } else if (bytesWritten == 0) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }

        } else if (keySession.progressState == Progress.WRITING_FILELIST) {
            if (keySession.fileChannel == null || !keySession.fileChannel.isOpen())
                return;
            long bytesWritten;
            bytesWritten = keySession.fileChannel.transferTo(keySession.c2cTransferCurrentPosition,
                    keySession.fileChannel.size(), clientChannel);
            if(bytesWritten < 0){
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                cancelKey(key);
            }else if(bytesWritten > 0){
                keySession.c2cTransferCurrentPosition += bytesWritten;
                if(keySession.c2cTransferCurrentPosition == keySession.fileChannel.size()){
                    keySession.fileChannel.close();
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }else if(bytesWritten == 0){
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        } else {
            try {
                keySession.fileChannel = FileChannel.open(keySession.fileListTempFile, StandardOpenOption.READ);
                keySession.progressState = Progress.FILE_LIST_SAVED_TO_DISK;
            } catch (Exception e) {
                System.out.println("An error occured while sending file list to client ");
            }
        }
    }

    // resume work here
    private static long serverSendFile(String fileName, SocketChannel clientChannel, ByteBuffer[] buffersArr) {
        ByteBuffer commandBuffer = buffersArr[0];
        ByteBuffer fileNameLengthBuffer = buffersArr[1];
        ByteBuffer fileNameBuffer = buffersArr[2];
        ByteBuffer sendBuffer = buffersArr[3];
        Path fileToSend = serverDownloadPath.resolve(fileName);
        long bytesWritten;
        try {
            if (Files.notExists(fileToSend)) {
                String information = fileName + " does not exist.";
                commandBuffer.clear().putInt(INFORMATION).flip();
                fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
                fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
                sendBuffer.clear().put(information.getBytes(StandardCharsets.UTF_8)).flip();
                ByteBuffer[] buffers = { commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer };
                try {
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                    System.err.println(
                            "An error occured while trying to send information to " + clientChannel.getRemoteAddress());
                }
            } else if (Files.exists(fileToSend)) {
                try (FileChannel fileChannel = FileChannel.open(fileToSend, StandardOpenOption.READ)) {
                    commandBuffer.clear().putInt(FILE_DOWNLOAD).flip();
                    fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
                    fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
                    MappedByteBuffer fileDataBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0,
                            fileChannel.size());
                    ByteBuffer[] buffers = { commandBuffer, fileNameLengthBuffer, fileNameBuffer, fileDataBuffer };
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                    System.err.println("An error occured while trying to send file " + fileName + " to "
                            + clientChannel.getRemoteAddress());
                }
            } else {
                String information = "Server could not send file" + fileName;
                commandBuffer.clear().putInt(INFORMATION).flip();
                fileNameBuffer.clear().put(fileName.getBytes(StandardCharsets.UTF_8));
                fileNameLengthBuffer.clear().putInt(fileNameBuffer.capacity()).flip();
                sendBuffer.clear().put(information.getBytes(StandardCharsets.UTF_8)).flip();
                ByteBuffer[] buffers = { commandBuffer, fileNameLengthBuffer, fileNameBuffer, sendBuffer };
                try {
                    bytesWritten = clientChannel.write(buffers);
                    return bytesWritten;
                } catch (IOException e) {
                    System.err.println(
                            "An error occured while trying to send information to " + clientChannel.getRemoteAddress());
                }
            }
        } catch (IOException e) {
        }
        return bytesWritten = 0L;
    }

    private static void serverReceiveFileData(){}

    private static void serverSaveFileData(String fileName, ByteBuffer fileDataBuffer) {
        Path filePath = serverDownloadPath.resolve(fileName);
        int lastDotIndex = fileName.lastIndexOf(".");
        String fileExtension = fileName.substring(lastDotIndex);
        String fileNameWithouINFORMATIONension = fileName.substring(0, lastDotIndex);
        int counter = 0;
        while (true) {
            if (Files.exists(filePath)) {
                counter++;
                filePath = serverDownloadPath
                        .resolve(fileNameWithouINFORMATIONension + "(" + counter + ")" + fileExtension);
            } else
                break;
        }

        try (AsynchronousFileChannel asyncfileChannel = AsynchronousFileChannel.open(filePath,
                StandardOpenOption.WRITE)) {
            asyncfileChannel.write(fileDataBuffer, 0, null, new CompletionHandler<Integer, Void>() {
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

    private static void cancelKey(SelectionKey key) {
        try {
            key.cancel();
            key.channel().close();
        } catch (Exception e) {
            System.err.println("An error occured while trying to cancel key " + e.getMessage());
        }
    }

    // private static byte[] encrypt(){}

    // private static byte[] decrypt(){}

    private static class CurrentSession {
        Progress progressState;
        ByteBuffer handShakeSendBuffer;
        ByteBuffer handShakeReceiveBuffer;
        ByteBuffer sendBuffer = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer commandBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileNameLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer handShakeSendLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer handShakeReceiveLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileListLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileSizeBuffer = ByteBuffer.allocate(8);
        ByteBuffer fileNameBuffer;
        ByteBuffer fileDataBuffer;
        FileChannel fileChannel;
        Path fileListTempFile;
        int fileListStringLength;
        long c2cTransferCurrentPosition = 0;
        long c2cTransferRemainingBytes = 0;

    }

}
