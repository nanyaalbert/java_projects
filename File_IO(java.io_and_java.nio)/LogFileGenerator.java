import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LogFileGenerator {
    private static final String LOG_FILE_PATH = "application_log.txt";
    private static File file = new File(LOG_FILE_PATH);

    public static void generateLogFile(){
        try {
            file.createNewFile();
            System.out.println("Log file created at: " + file.getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Error creating log file: " + ex.getMessage());
        }
    }

    public static void writeToLog(String logLevel, String message){
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            String logEntry = String.format("[%s] : [%s] - %s", 
                                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                                            logLevel.toUpperCase(), 
                                            message);
            writer.println(logEntry);
            System.out.println("Log entry added: " + logEntry);            
        } catch (IOException ex) {
            System.out.println("Error writing to log file: " + ex.getMessage());
        }        
    }

}

