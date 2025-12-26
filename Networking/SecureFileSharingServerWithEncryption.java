import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecureFileSharingServerWithEncryption {
    private static final int PORT = 1234;
    private static final int NO_COMMAND = 0;
    private static final int FILE_SEND_REQUEST = 1;
    private static final int FILE_UPLOAD_REQUEST = 2;
    private static final int FILE_DOWNLOAD = 3;
    private static final int FILE_LIST_REQUEST = 4;
    private static final int FILE_LIST = 5;
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;
    private static final long MAX_ENCRYPTED_FILE_SIZE = MAX_FILE_SIZE + 64; // 64 bytes safety margin
    private static final int MAX_FILE_NAME_LENGTH = 512;
    private static final int MAX_ENCRYPTED_FILE_NAME_LENGTH = MAX_FILE_NAME_LENGTH + 64; // 64 bytes safety margin
    private static String HANDSHAKE_STRING = "SecureFileSharingHandShake";
    private static long TEMPFILENUMBER = 0;
    private static Path serverDownloadPath = Paths.get(System.getProperty("user.home"),
            "SecureFileSharingServerWithEncryption");
    private static Path serverTempPath = Paths.get(System.getProperty("user.home"),
            "SecureFileSharingServerWithEncryptionTemp");

    private enum Progress {
        JUST_CONNECTED,
        READY_TO_READ_HANDSHAKE,
        READING_FILEDATA,
        READING_FILEDETAILS,
        READING_INFORMATION,
        READING_HANDSHAKE,
        READING_FILELIST,
        WRITING_FILEDATA,
        WRITING_FILEDETAILS,
        WRITING_INFORMATION,
        WRITING_HANDSHAKE,
        WRITING_FILELIST,
        VALID_HANDSHAKE,
        FILE_LIST_SAVED_TO_DISK
    }

    public static void main(String[] args) {
        server();
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
            InetSocketAddress serverAddress = new InetSocketAddress("0.0.0.0", PORT);
            serverChannel.bind(serverAddress);
            printConnectionGuide();
            Scanner userInput = new Scanner(System.in);
            System.out.println("Please setup a password for the server.");
            System.out.println("Enter password: ");
            HANDSHAKE_STRING += userInput.nextLine();
            System.out.println("\nWaiting for connections...");
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

                        switch (keySession.progressState) { // remember to add all the other cases for this enum
                            case Progress.JUST_CONNECTED, Progress.WRITING_HANDSHAKE -> {
                                writeHandShake(key);
                            }
                            case Progress.READY_TO_READ_HANDSHAKE, Progress.READING_HANDSHAKE -> {
                                readHandShake(key);
                            }
                            case Progress.VALID_HANDSHAKE -> {
                                if (keySession.command == NO_COMMAND) {
                                    readyClient.read(keySession.commandBuffer);
                                }
                                if (keySession.command == NO_COMMAND
                                        && keySession.commandBuffer.position() == keySession.commandBuffer.capacity()) {
                                    keySession.command = keySession.commandBuffer.getInt();
                                } else
                                    continue;
                                if (keySession.command == FILE_SEND_REQUEST) {
                                    serverSendFile(key);
                                }
                                if (keySession.command == FILE_UPLOAD_REQUEST) {
                                    serverReceiveFile(key);
                                }
                                if (keySession.command == FILE_LIST_REQUEST) {
                                    serverSendFilesList(key);
                                }
                            }
                            case Progress.WRITING_FILEDETAILS, Progress.WRITING_FILEDATA -> {
                                serverSendFile(key);
                            }
                            case Progress.READING_FILEDATA -> {
                                serverReceiveFile(key);
                            }
                            case Progress.FILE_LIST_SAVED_TO_DISK, Progress.WRITING_FILELIST -> {
                                serverSendFilesList(key);
                            }

                        }

                    }
                }
            }

        } catch (Exception e) {
            System.out.println("An error occured with the server: " + e.getMessage());
        }

    }

    private static void readHandShake(SelectionKey key) throws IOException {
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

    private static void writeHandShake(SelectionKey key) throws IOException {
        long bytesWritten;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        CurrentSession keySession = (CurrentSession) key.attachment();
        int prevOps = key.interestOps();
        key.interestOps(0);
        try {
            ByteBuffer[] handshakeBufferArrs = { keySession.handShakeSendLengthBuffer, keySession.handShakeSendBuffer };
            bytesWritten = clientChannel.write(handshakeBufferArrs);
            if (bytesWritten < 0) {
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                cancelKey(key);
            } else if (bytesWritten > 0 && keySession.handShakeSendBuffer.hasRemaining()) {
                keySession.progressState = Progress.WRITING_HANDSHAKE;
            } else if (bytesWritten > 0 && !keySession.handShakeSendBuffer.hasRemaining()) {
                keySession.handShakeSendLengthBuffer.clear();
                keySession.handShakeSendBuffer.clear();
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                key.interestOps(prevOps);
                keySession.progressState = Progress.READY_TO_READ_HANDSHAKE;
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
            if (keySession.fileListInfoHeaderBufferArr == null) {
                keySession.fileListInfoHeaderBufferArr = new ByteBuffer[] { keySession.commandBuffer,
                        keySession.fileListLengthBuffer };
            }
            bytesWritten = clientChannel.write(keySession.fileListInfoHeaderBufferArr);
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
            if (bytesWritten < 0) {
                System.err.println("Client " + clientChannel.getRemoteAddress() + " closed the connection");
                cancelKey(key);
            } else if (bytesWritten > 0) {
                keySession.c2cTransferCurrentPosition += bytesWritten;
                if (keySession.c2cTransferCurrentPosition == keySession.fileChannel.size()) {
                    keySession.fileChannel.close();
                    resetCurrentSessionObj(keySession);
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                }
            } else if (bytesWritten == 0) {
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

    private static void serverSendFile(SelectionKey key) throws IOException {
        int bytesRead;
        SocketChannel readyClient = (SocketChannel) key.channel();
        CurrentSession keySession = (CurrentSession) key.attachment();
        if (keySession.fileNameLengthBuffer.position() < 4) {
            bytesRead = readyClient.read(keySession.fileNameLengthBuffer);
            if (bytesRead < 0) {
                cancelKey(key);
                return;
            }
            if (keySession.fileNameLengthBuffer.position() < 4)
                return;
        }
        if (keySession.fileNameLengthBuffer.position() == 4
                && keySession.fileNameLength == 0) {
            keySession.fileNameLength = keySession.fileNameLengthBuffer.getInt();
        }
        if (keySession.fileNameLength > 0) {
            keySession.fileNameBuffer = ByteBuffer.allocate(keySession.fileNameLength);
            bytesRead = readyClient.read(keySession.fileNameBuffer);
            if (bytesRead < 0) {
                cancelKey(key);
                return;
            }
            if (keySession.fileNameBuffer.position() < keySession.fileNameLength)
                return;
        }
        if (keySession.fileNameBuffer.position() == keySession.fileNameLength
                && keySession.fileName.isEmpty()) {
            keySession.fileNameBuffer.flip();
            keySession.fileName = new String(keySession.fileNameBuffer.array(),
                    StandardCharsets.UTF_8);
        }
        if (keySession.fileSize == 0) {
            Path filePath = serverDownloadPath.resolve(keySession.fileName);
            if (Files.notExists(filePath)) {
                long bytesWritten;
                if (keySession.information.isEmpty()) {
                    keySession.information = "file \" " + keySession.fileName
                            + "\" does not exist";
                    keySession.informationBuffer = ByteBuffer
                            .wrap(keySession.information.getBytes(StandardCharsets.UTF_8));
                    keySession.informationSizeBuffer.clear()
                            .putInt(keySession.informationBuffer.capacity()).flip();
                    keySession.informationBufferArr = new ByteBuffer[] {
                            keySession.informationSizeBuffer,
                            keySession.informationBuffer };
                } else if (!keySession.information.isEmpty()
                        && keySession.informationBufferArr != null) {
                    bytesWritten = readyClient.write(keySession.informationBufferArr);
                    if (bytesWritten < 0) {
                        cancelKey(key);
                        return;
                    } else if (bytesWritten > 0
                            && !keySession.informationBuffer.hasRemaining()) {
                        resetCurrentSessionObj(keySession);
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    } else if (bytesWritten == 0) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
            } else if (Files.exists(filePath)) {
                if (keySession.fileChannel == null || !keySession.fileChannel.isOpen()) {
                    keySession.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
                    keySession.fileSize = keySession.fileChannel.size();
                    keySession.commandBuffer.putInt(FILE_DOWNLOAD);
                    keySession.fileDetailsBufferArr = new ByteBuffer[] { keySession.commandBuffer.flip(),
                            keySession.fileNameLengthBuffer.flip(), keySession.fileNameBuffer.flip() };
                    keySession.progressState = Progress.WRITING_FILEDETAILS;
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
                if (keySession.progressState == Progress.WRITING_FILEDETAILS) {
                    long bytesWritten = readyClient.write(keySession.fileDetailsBufferArr);
                    if (bytesWritten < 0) {
                        cancelKey(key);
                        return;
                    } else if (bytesWritten > 0 && !keySession.fileNameBuffer.hasRemaining()) {
                        keySession.progressState = Progress.WRITING_FILEDATA;
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    } else if (bytesWritten == 0) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
                if (keySession.progressState == Progress.WRITING_FILEDATA) {
                    long bytesWritten;
                    bytesWritten = keySession.fileChannel.transferTo(keySession.c2cTransferCurrentPosition,
                            keySession.fileChannel.size(), readyClient);
                    if (bytesWritten < 0) {
                        System.err.println("Client " + readyClient.getRemoteAddress() + " closed the connection");
                        cancelKey(key);
                    } else if (bytesWritten > 0) {
                        keySession.c2cTransferCurrentPosition += bytesWritten;
                        if (keySession.c2cTransferCurrentPosition == keySession.fileChannel.size()) {
                            keySession.fileChannel.close();
                            resetCurrentSessionObj(keySession);
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                    } else if (bytesWritten == 0) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
            }
        }
    }

    private static void serverReceiveFile(SelectionKey key) throws IOException {
        int bytesRead;
        SocketChannel readyClient = (SocketChannel) key.channel();
        CurrentSession keySession = (CurrentSession) key.attachment();

        if (keySession.fileNameLengthBuffer.position() < 4) {
            bytesRead = readyClient.read(keySession.fileNameLengthBuffer);
            if (bytesRead < 0) {
                cancelKey(key);
                return;
            }
            if (keySession.fileNameLengthBuffer.position() < 4)
                return;
        }
        if (keySession.fileNameLengthBuffer.position() == 4
                && keySession.fileNameLength == 0) {
            keySession.fileNameLength = keySession.fileNameLengthBuffer.getInt();
        }
        if (keySession.fileSizeBuffer.position() < 8) {
            bytesRead = readyClient.read(keySession.fileSizeBuffer);
            if (bytesRead < 0) {
                cancelKey(key);
                return;
            }
            if (keySession.fileSizeBuffer.position() < 8)
                return;
        }
        if (keySession.fileNameLength > 0) {
            keySession.fileNameBuffer = ByteBuffer.allocate(keySession.fileNameLength);
            bytesRead = readyClient.read(keySession.fileNameBuffer);
            if (bytesRead < 0) {
                cancelKey(key);
                return;
            }
            if (keySession.fileNameBuffer.position() < keySession.fileNameLength)
                return;
        }
        if (keySession.fileNameBuffer.position() == keySession.fileNameLength
                && keySession.fileName.isEmpty()) {
            keySession.fileNameBuffer.flip();
            keySession.fileName = new String(keySession.fileNameBuffer.array(),
                    StandardCharsets.UTF_8);
        }
        if (keySession.fileSizeBuffer.position() == 8
                && keySession.fileSize == 0) {
            keySession.fileSize = keySession.fileSizeBuffer.getLong();
        }
        if (!keySession.fileName.isEmpty() && keySession.fileSize != 0) {
            Path filePath = serverDownloadPath.resolve(keySession.fileName);
            int lastDotIndex = keySession.fileName.lastIndexOf(".");
            keySession.fileExtension = keySession.fileName.substring(lastDotIndex);
            keySession.fileNameWithoutExtension = keySession.fileName.substring(0, lastDotIndex);
            if (keySession.progressState != Progress.READING_FILEDATA) {
                int counter = 0;
                while (true) {
                    if (Files.exists(filePath)) {
                        counter++;
                        filePath = serverDownloadPath
                                .resolve(keySession.fileNameWithoutExtension + "(" + counter + ")"
                                        + keySession.fileExtension);
                        keySession.fileName = filePath.getFileName().toString();
                        int lastDotIndexNew = filePath.getFileName().toString().lastIndexOf(".");
                        keySession.fileExtension = filePath.getFileName().toString().substring(lastDotIndexNew);
                        keySession.fileNameWithoutExtension = filePath.getFileName().toString().substring(0,
                                lastDotIndexNew);
                    } else {
                        try {
                            Files.createFile(filePath);
                            keySession.progressState = Progress.READING_FILEDATA;
                        } catch (Exception e) {
                            System.err.println("Failed to create file: " + filePath.toAbsolutePath());
                        }
                        break;
                    }
                }
            }
            if (keySession.progressState == Progress.READING_FILEDATA) {
                if (!keySession.fileChannel.isOpen()) {
                    try {
                        keySession.fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE);
                    } catch (Exception e) {
                        System.err.println(
                                "An error occured while trying to open channel on " + filePath.toAbsolutePath());
                        return;
                    }
                }
                if (keySession.fileChannel.isOpen()) {
                    long bytesReadToFile;
                    bytesReadToFile = keySession.fileChannel.transferFrom(readyClient,
                            keySession.c2cTransferCurrentPosition, keySession.fileSize);
                    if (bytesReadToFile < 0) {
                        keySession.fileChannel.close();
                        cancelKey(key);
                        return;
                    }
                    if (bytesReadToFile > 0) {
                        keySession.c2cTransferCurrentPosition += bytesReadToFile;
                        if (keySession.c2cTransferCurrentPosition == keySession.fileSize) {
                            System.out.println("File transfer completed for " + keySession.fileName);
                            try {
                                keySession.fileChannel.close();
                            } catch (Exception e) {
                                System.err.println("Failed to close file channel for " + keySession.fileName);
                            }
                            resetCurrentSessionObj(keySession);
                        }
                    }
                    if (bytesReadToFile == 0) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                    }
                }
            }
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

    private static void printConnectionGuide() {
        String bestIP = null;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (netint.isLoopback() || !netint.isUp() || netint.isVirtual())
                    continue;

                String name = netint.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vmware") || name.contains("docker")
                        || name.contains("vbox") || name.contains("vm"))
                    continue;

                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddr : Collections.list(inetAddresses)) {
                    if (inetAddr instanceof Inet4Address) {
                        bestIP = inetAddr.getHostAddress();
                        break;
                    }
                }
                if (bestIP != null)
                    break;
            }
        } catch (Exception e) {
            // Fallback to null logic handled below
        }

        System.out.println("\n-------------------------------------------");
        System.out.println("SERVER INITIALIZED ON PORT: " + PORT);

        if (bestIP != null) {
            System.out.println("STATUS: Network Active");
            System.out.println("-> Connect from other PCs on the same network via: " + bestIP);
            System.out.println("-> Connect from THIS PC via:    localhost");
        } else {
            System.out.println("STATUS: Offline / No Network Found");
            System.out.println("-> Only programs on THIS computer can connect.");
            System.out.println("-> Use: localhost (127.0.0.1)");
        }
        System.out.println("-------------------------------------------\n");
    }

    // private static byte[] encrypt(){}

    // private static byte[] decrypt(){}

    private static void resetCurrentSessionObj(CurrentSession keySession) {
        keySession.progressState = Progress.VALID_HANDSHAKE;
        keySession.handShakeSendBuffer = null;
        keySession.handShakeReceiveBuffer = null;
        keySession.informationBuffer = null;
        keySession.fileNameBuffer = null;
        keySession.sendBuffer.clear();
        keySession.receiveBuffer.clear();
        keySession.commandBuffer.clear();
        keySession.fileNameLengthBuffer.clear();
        keySession.handShakeSendLengthBuffer.clear();
        keySession.handShakeReceiveLengthBuffer.clear();
        keySession.fileListLengthBuffer.clear();
        keySession.informationSizeBuffer.clear();
        keySession.fileSizeBuffer.clear();
        keySession.fileListInfoHeaderBufferArr = null;
        keySession.informationBufferArr = null;
        keySession.fileDetailsBufferArr = null;
        keySession.fileChannel = null;
        keySession.fileListTempFile = null;
        keySession.command = NO_COMMAND;
        keySession.fileListStringLength = 0;
        keySession.fileNameLength = 0;
        keySession.fileSize = 0;
        keySession.c2cTransferCurrentPosition = 0;
        keySession.information = "";
        keySession.fileName = "";
        keySession.fileNameWithoutExtension = "";
        keySession.fileExtension = "";
    }

    private static class CurrentSession {
        Progress progressState = null;
        ByteBuffer handShakeSendBuffer = null;
        ByteBuffer handShakeReceiveBuffer = null;
        ByteBuffer informationBuffer = null;
        ByteBuffer fileNameBuffer = null;
        ByteBuffer sendBuffer = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer commandBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileNameLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer handShakeSendLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer handShakeReceiveLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileListLengthBuffer = ByteBuffer.allocate(4);
        ByteBuffer informationSizeBuffer = ByteBuffer.allocate(4);
        ByteBuffer fileSizeBuffer = ByteBuffer.allocate(8);
        ByteBuffer[] fileListInfoHeaderBufferArr = null;
        ByteBuffer[] informationBufferArr = null;
        ByteBuffer[] fileDetailsBufferArr = null;
        FileChannel fileChannel = null;
        Path fileListTempFile = null;
        int command = NO_COMMAND;
        int fileListStringLength = 0;
        int fileNameLength = 0;
        long fileSize = 0;
        long c2cTransferCurrentPosition = 0;
        String information = "";
        String fileName = "";
        String fileNameWithoutExtension = "";
        String fileExtension = "";
    }

}
