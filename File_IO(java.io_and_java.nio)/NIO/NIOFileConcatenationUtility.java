import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class NIOFileConcatenationUtility {
    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("Please provide at least two .txt file paths.");
            return;
        }

        else {
            File concatenatedFile = new File("concatenated_file.txt");
            File[] inputFilesArray = getInputFiles(args);
            if(allFilesExist(inputFilesArray)){
                concatenateFiles(inputFilesArray, concatenatedFile);
            } else System.out.println("One or more .txt input files do not exist. please check the file paths.");
        }

    }

    private static File[] getInputFiles(String[] inputFilesPath){
        File[] inputFiles = new File[inputFilesPath.length];
        for(int i = 0; i < inputFilesPath.length; i++){
            inputFiles[i] = new File(inputFilesPath[i]);
        }
        return inputFiles;

    }

    private static boolean allFilesExist(File[] files){
        for(File currentFile : files){
            if(!currentFile.exists() || (currentFile.exists() && !currentFile.getName().endsWith(".txt"))){
                return false;
            }
        }
        return true;
    }

    private static void concatenateFiles(File[] inputFiles, File outputFile){
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            FileChannel outputChannel = output.getChannel();
            for (File currentInputFile : inputFiles){
                try (RandomAccessFile input = new RandomAccessFile(currentInputFile, "r")) {
                    FileChannel currentInputChannel = input.getChannel();
                    currentInputChannel.transferTo(0, currentInputChannel.size(), outputChannel);
                    currentInputChannel.close();
                } catch (IOException ex) {
                    System.out.println("An error occured while processing file " + currentInputFile.getAbsolutePath() + ": " + ex.getMessage());
                    return;
                }
            }
            System.out.println("Files concatenated successfully into " + outputFile.getAbsolutePath());
            outputChannel.close();
            
        } catch (IOException ex) {
            System.out.println("An error occured during file concatenation: " + ex.getMessage());
            return;
        }

    }

}
