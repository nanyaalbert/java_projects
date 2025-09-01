import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class RobustTextFileCopier {
    public static void main(String[] args){
        if(args.length == 2){
            String sourcePath = args[0];
            String destPath = args[1];
            File sourceFile = new File(sourcePath);
            File destFile = new File(destPath);
            if(sourceFile.exists() && isTextFile(sourceFile)){
                if(!destFile.exists() && isTextFile(destFile)) {
                    try {
                        System.out.println("Destination file does not exist. Creating: " + destFile.getAbsolutePath());
                        destFile.createNewFile();
                        System.out.println("File creation was completed successfully.");
                        copyTextFile(sourceFile, destFile);
                    } catch (IOException e) {
                        System.out.println("Failed to create destination file: " + e.getMessage());
                        return;
                    }
                } else if(destFile.exists() && isTextFile(destFile)){
                    Scanner scanner = new Scanner(System.in);
                    String response;
                    System.out.println("Destination file already exists and is a text file: " + destFile.getAbsolutePath());
                    System.out.println("Do you want to overwrite it? (yes/no)");            
                    while(true){
                        response = scanner.nextLine().trim().toLowerCase();
                        if(response.equals("yes")) {
                            System.out.println("Overwriting file...");
                            copyTextFile(sourceFile, destFile);
                            System.out.println("File contents copied successfully.");
                            break;
                        }
                        else if(response.equals("no")) {
                            System.out.println("File copy operation cancelled.");
                            break;
                        }
                        else{
                            System.out.println("Invalid response. Please enter 'yes' or 'no'.");
                        }
                    }
                    scanner.close();
                } else if(destFile.exists() && !isTextFile(destFile)) System.out.println("Destination file is not a text file: " + destFile.getAbsolutePath());

            } else if(!sourceFile.exists()) System.out.println("Source file does not exist: " + sourceFile.getAbsolutePath());
            else if(!isTextFile(sourceFile)) System.out.println("Source file is not a text file: " + sourceFile.getAbsolutePath());
        } else{
            System.out.println("Usage: java RobustTextFileCopier <source_file_path> <destination_file_path>");
            System.out.println("Please provide the source and destination file paths as command line arguments.");
        }
    }

    private static boolean isTextFile(File file) {
        return file.getName().toLowerCase().endsWith(".txt");
    }

    private static void copyTextFile(File sourceFile, File destFile){
        if(sourceFile.canRead() && destFile.canWrite()){
            try (
                BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(destFile)));
            ) {
                System.out.println("Copying contents from " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                String line;
                while((line = reader.readLine()) != null){
                    writer.println(line);
                }
                System.out.println("Successfully Copied contents from " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
            } catch (IOException ex) {
                System.out.println("An error occured while copying the file: " + ex.getMessage());
            }
        }
        else if(!sourceFile.canRead()) {
            System.out.println("Source file " + sourceFile.getName() + " cannot be read: " + sourceFile.getAbsolutePath());
        } else if (!destFile.canWrite()) {
            System.out.println("Destination file " + destFile.getName() + " cannot be written to: " + destFile.getAbsolutePath());
        }
    }

}

