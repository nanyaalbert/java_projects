# Description
This directory focuses on file input/output operations in Java using both java.io and java.nio packages.

## NIO/RealTimeDirectoryMonitorAndAsynchronousLogger.java
### Overview
The Real-Time Directory Monitor and Asynchronous Logger is a Java program that monitors a specified directory for file creation, modification, and deletion events in real-time using Java NIO's WatchService. It logs these events asynchronously to log_file.txt with timestamps, using AsynchronousFileChannel for efficient, non-blocking I/O.

### Features
1. Monitors file creation (ENTRY_CREATE), modification (ENTRY_MODIFY), and deletion (ENTRY_DELETE) in the specified directory.
2. Logs events to log_file.txt with timestamps in the format [yyyy-MM-dd HH:mm:ss] - Event kind: \<kind\>. File affected: \<file\>.
3. Uses asynchronous I/O for logging to minimize performance impact.
4. Validates directory existence and type before monitoring.
5. Logs monitoring setup and errors to the console.

### Notes
- Monitors only files directly in the specified directory, not subdirectories.
- Creates log_file.txt in the current working directory if it doesn't exist.
- Handles OVERFLOW events and invalid WatchKey states.
- Handles I/O and interruption exceptions.

### Usage
1. **Compile:** javac RealTimeDirectoryMonitorAndAsynchronousLogger.java
2. **Run:** java RealTimeDirectoryMonitorAndAsynchronousLogger \<directory-path\> Example: java RealTimeDirectoryMonitorAndAsynchronousLogger /home/user/docs

## NIO/MediaFileOrganizer.java
### Overview
The Media File Organizer is a Java utility that organizes media files (images, videos, audio) into a structured hierarchy based on file type and creation date. It supports formats like .jpg, .png, .mp4, .mkv, .mp3, and .wav, and creates a log file (media_organizer_log.txt) to track actions and errors. The program handles duplicates, deletes empty directories, and ensures proper permissions.

### Features

1. Organizes files into Media/Images, Media/Videos, and Media/Audio by year and month.
2. Supports: Images (.jpg, .jpeg, .png, .gif, .webp, .tiff, .svg), Videos (.mp4, .mov, .mkv, .avi), Audio (.mp3, .wav).
3. Renames duplicate files to avoid conflicts.
4. Logs all actions (scanning, moving, errors) in media_organizer_log.txt.
5. Deletes empty directories post-processing.
6. Validates directory permissions.

### Notes
- Skips media_organizer_log.txt during processing.
- Logs and skips unsupported file types or files with inaccessible creation dates.
- Handles I/O exceptions.

### Usage
1. **Compile:** javac MediaFileOrganizer.java
2. **Run:** java MediaFileOrganizer \<directory-path\> Example: java MediaFileOrganizer /home/user/photos

## NIO/SimpleFileEncryptionAndDecryption.java
### Overview
The Simple File Encryption and Decryption is a Java utility that encrypts or decrypts a file using a simple XOR operation with a user-provided byte key. It uses NIO FileChannel to read and write file contents in-place, supporting both encryption and decryption modes.

### Features
1. Encrypts or decrypts files using XOR with a byte key (-128 to 127).
2. Operates in-place on the input file using NIO FileChannel.
3. Validates input file existence and key format.
4. Logs success or errors to the console.


### Notes
- The same key must be used for encryption and decryption.
- Invalid modes (other than encrypt or decrypt) are rejected.

### Usage
1. **Compile:** javac SimpleFileEncryptionAndDecryption.java
2. **Run:** java SimpleFileEncryptionAndDecryption \<encrypt|decrypt\> \<key\> \<input-file\> Example: java SimpleFileEncryptionAndDecryption encrypt 42 document.txt

## NIO/DirectoryFileFinderAndTruncator.java
### Overview
The Directory File Finder and Truncator is a Java utility that searches a specified directory and its subdirectories for files with a given file extension and truncates their contents to zero bytes. It uses Java NIO to efficiently locate and modify files, logging each truncation or error to the console.

### Features
1. Searches for files with a specified extension (e.g., .txt).
2. Truncates matching files to zero bytes using NIO FileChannel.
3. Validates directory existence, directory type, and file extension format.
4. Logs truncation success or errors to the console.

### Notes
- Requires a valid directory path and a file extension starting with a dot (e.g., .txt).
- Skips non-regular files and empty files.
- Handles I/O exceptions and logs errors during file searching or truncation.

### Usage
1. **Compile:** javac DirectoryFileFinderAndTruncator.java
2. **Run:** java DirectoryFileFinderAndTruncator \<directory-path\> \<file-extension\> Example: java DirectoryFileFinderAndTruncator /home/user/docs .txt

## NIO/FileCopyWithScatterGatherIO.java
### Overview
The File Copy with Scatter-Gather I/O is a Java program that copies the contents of a source file to a destination file using NIO scatter-gather I/O. It splits the file into a header (first 12 bytes) and body, processes them in separate buffers, and writes them to the destination file.

### Features
1. Uses NIO FileChannel for scatter-gather I/O operations.
2. Splits file content into a 12-byte header and the remaining body.
3. Validates existence of source and destination files.
4. Logs scatter and gather operations to the console.

### Notes
- Both source and destination files must exist.
- Handles I/O exceptions and logs errors during the copy process.
- Assumes the destination file is writable.

### Usage
1. **Compile:** javac FileCopyWithScatterGatherIO.java
2. **Run:** java FileCopyWithScatterGatherIO \<source-file\> \<destination-file\> Example: java FileCopyWithScatterGatherIO input.txt output.txt

## NIO/LargeFileViewerWithMemoryMapping.java
### Overview
The Large File Viewer with Memory Mapping is a Java program that displays a specific page of a large text file using NIO memory-mapped I/O. It maps a 4096-byte page (specified by page number) into memory and prints its contents, designed for efficient access to large files.

### Features
1. Displays a specific 4096-byte page of a text file using MappedByteBuffer.
2. Validates file existence, text file format (.txt), and page number.
3. Logs page content or errors to the console.

### Notes
- Page numbers start at 1; invalid page numbers are rejected.
- Only .txt files are supported.
- Handles partial pages at the file’s end and I/O exceptions.

### Usage
1. **Compile:** javac LargeFileViewerWithMemoryMapping.java
2. ****Run:**** java LargeFileViewerWithMemoryMapping \<file-path\> \<page-number\> Example: java LargeFileViewerWithMemoryMapping document.txt 2

## NIO/NIOFileConcatenationUtility.java
### Overview
The NIO File Concatenation Utility is a Java program that concatenates multiple text files into a single output file (concatenated_file.txt) using NIO FileChannel. It validates input files and logs the success or failure of the concatenation process.

### Features
1. Concatenates multiple .txt files into one output file.
2. Uses NIO FileChannel for efficient file transfer.
3. Validates that all input files exist and are text files.
4. Logs concatenation success or errors to the console.

### Notes
- Requires at least two input files.
- Only .txt files are supported.

### Usage
1. ****Compile:**** javac NIOFileConcatenationUtility.java
2. **Run:** java NIOFileConcatenationUtility \<file1\> \<file2\> [file3 ...]Example: java NIOFileConcatenationUtility file1.txt file2.txt

## NIO/SharedFileAccessWithLocking.java
### Overview
The Shared File Access with Locking is a Java program that enables concurrent read or write access to a file using NIO FileLock. It supports reading integers from a file or writing random integers, with file locking to prevent race conditions, and logs actions to the console.

### Features
1. Supports read (r) or write (w) modes with file locking.
2. Reads integers or writes random integers (0-99) to the file.
3. Uses NIO FileChannel and FileLock for safe concurrent access.
4. Logs lock acquisition, read/write operations, and errors.


### Notes
- The file must exist and be a regular file.
- Write mode generates random integers; read mode expects integer data.
- Runs indefinitely until interrupted, with random sleep intervals (500-1000ms).
- Run multiple instances with different modes to observe how the file lock operates in action.

### Usage
1. **Compile:** javac SharedFileAccessWithLocking.java
2. **Run:** java SharedFileAccessWithLocking \<r|w\> \<file-path\> Example: java SharedFileAccessWithLocking w data.bin

## BinaryDataProcessor.java
### Overview
The Binary Data Processor is a Java program that generates an array of random doubles, writes them to a binary file (data.bin), and reads them back, doubling each value during output. It uses DataOutputStream and DataInputStream for binary I/O and logs actions to the console.

### Features
1. Generates 10 random doubles \[1.0, 11.0\) and writes them to data.bin.
2. Reads doubles from data.bin and prints each multiplied by 2.0.
3. Uses DataOutputStream and DataInputStream for binary file operations.
4. Logs write/read success or errors to the console.

### Notes
- The program creates data.bin in the current working directory.
- Handles EOFException to stop reading at file end.
- Output file must be writable; input file must exist for reading.

### Usage
1. **Compile:** javac BinaryDataProcessor.java
2. **Run:** java BinaryDataProcessor

## CustomObjectPersistence.java
### Overview
The Custom Object Persistence is a Java program that demonstrates serialization and deserialization of a list of Product objects to and from a binary file (productlist.bin). It creates a list of products, serializes them to a file, and then deserializes and prints them, logging actions to the console.

### Features
1. Serializes an ArrayList\<Product\> to productlist.bin using ObjectOutputStream.
2. Deserializes the product list from productlist.bin using ObjectInputStream.
3. Includes a Product class implementing Serializable with fields: id, name, price, quantity.
4. Logs serialization/deserialization success or errors to the console.

### Notes
- The program creates a sample product list with two products.
- Handles I/O and ClassNotFoundException errors.
- The output file (productlist.bin) is created in the current working directory.

### Usage
1. **Compile:** javac CustomObjectPersistence.java
2. **Run:** java CustomObjectPersistence

## FileMetadataExplorer.java
### Overview
The File Metadata Explorer is a Java program that retrieves and displays metadata for a specified file or directory. It provides details such as file type, size, permissions, last modified date, and directory contents (if applicable), using a console-based interface.

### Features
1. Displays metadata: file/directory type, absolute path, size, readability, writability, executability, and last modified date.
2. For directories, lists contents and total file/directory count.
3. Calculates directory size recursively.
4. Accepts file path input via Scanner.

### Notes
- Validates file/directory existence before processing.
- Only processes regular files or directories.
- Directory size includes all nested files and subdirectories.

### Usage
1. **Compile:** javac FileMetadataExplorer.java
2. **Run:** java FileMetadataExplorer
3. Enter the file or directory path when prompted.

## LogFileGenerator.java
### Overview
The Log File Generator is a Java utility that creates a log file (application_log.txt) and writes timestamped log entries with specified log levels and messages. It uses PrintWriter and BufferedWriter for efficient file writing and logs actions to the console.

### Features
1. Creates application_log.txt if it doesn't exist.
2. Writes log entries in the format: [timestamp] : [LEVEL] - message.
3. Supports appending to the log file.
4. Logs file creation and entry addition to the console.

### Notes
- The log file is created in the current working directory.
- Handles I/O exceptions.
- Log levels (e.g., INFO, ERROR) are user-specified and converted to uppercase.

### Usage
1. **Compile:** javac LogFileGenerator.java
2. Use in code by calling LogFileGenerator.generateLogFile() and LogFileGenerator.writeToLog(logLevel, message). Example:

## RobustTextFileCopier.java
### Overview
The Robust Text File Copier is a Java program that copies the contents of a source text file to a destination text file. It supports .txt files, checks permissions, and prompts for overwrite confirmation if the destination file exists, ensuring robust file handling.

### Features
1. Copies .txt file contents using BufferedReader and PrintWriter.
2. Validates source file existence and text file format.
3. Creates destination file if it doesn't exist; prompts for overwrite if it does.
4. Checks read/write permissions before copying.
5. Logs copy progress and errors to the console.

### Notes
- Only .txt files are supported.
- Handles I/O exceptions and permission issues.
- Uses Scanner for user input on overwrite prompts.

### Usage
1. **Compile:** javac RobustTextFileCopier.java

2. **Run:** java RobustTextFileCopier \<source-file\> \<destination-file\> Example: java RobustTextFileCopier input.txt output.txt
