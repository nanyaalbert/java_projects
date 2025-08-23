import java.io.File;
import java.util.Date;
import java.util.Scanner;

public class FileMetadataExplorer {
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the File Metadata Explorer");
        System.out.println("Please enter the file path: ");
        String filePath = scanner.nextLine();
        System.out.println("Fetching metadata for: " + filePath);
        System.out.println(getMetadata(new File(filePath)));
        scanner.close();

    }

    public static String getMetadata(File file){
        if(file == null || !file.exists()){
            return "File does not exist";
        }
        else if(file.exists() && (file.isFile() || file.isDirectory())){
           StringBuilder filemetadata;
           filemetadata = new StringBuilder();
           filemetadata.append(file.isDirectory()? "File exists and is a Directory\n" : "File exists and is a regular file\n");;
           filemetadata.append("Absolute path: " + file.getAbsolutePath()+ "\n");
           filemetadata.append(file.isDirectory() ? "Size of directory: " + getDirectorySize(file) + " bytes\n" : "Size of file: " + file.length() + " bytes\n");
           filemetadata.append(file.canRead() ? "File is Readable\n" : "File is not Readable\n");
           filemetadata.append(file.canWrite() ? "File is Writable\n" : "File is not Writable\n");
           filemetadata.append(file.canExecute() ? "File is Executable\n" : "File is not Executable\n");
           filemetadata.append("Last Modified: " + new Date(file.lastModified()) + "\n");
           if(file.isDirectory()) filemetadata.append(getDirectoryContents(file));
                
            return filemetadata.toString();
        }
        else{
            return "Unknown file type";
        }
        
    }

    public static long getDirectorySize(File dir){
        long size = 0;
        if (dir.isDirectory()){
            for(File file : dir.listFiles()){
                size += file.isDirectory() ? getDirectorySize(file) : file.length();
            }
        }
        return size;
    }

    public static String getDirectoryContents(File dir){
        StringBuilder contents = new StringBuilder();
        if(dir.isDirectory() && dir.listFiles().length > 0){
            contents.append("The contents of this directory are:\n");
            int count = 0;
            for(File file : dir.listFiles()){
                if(file != null){
                    contents.append(file.getName()).append("\n");
                    count++;
                }
            }
            contents.append("Total files/directories in " + dir.getName() + ": ").append(count).append("\n");
        } else {
            contents.append("No contents in this directory\n");
        }
        return contents.toString();
    }

}
