import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class SimpleWebContentDownloader{
    private static URI uri;
    private static URL url;
    public static void main(String[] args){
       System.out.println("Enter a valid URL to download its content:");
       Scanner userInput = new Scanner(System.in);
       String urlString = userInput.nextLine();
       

       try {
        uri = URI.create(urlString);
        url = uri.toURL();
        URLConnection connection = url.openConnection();
        String contentType = connection.getContentType();
        System.out.println(contentType);
        if(contentType.startsWith("text")) handleText(connection);
        else if(contentType.startsWith("image")) handleImage(connection, contentType.substring(6));
        else {
            System.out.println("The Url does not seem to point to a valid text or image file");
        }
        } catch (Exception e) {
            System.out.println("An Error has occured: " + e.getMessage());
        }
        userInput.close();
    }

    public static void handleText(URLConnection connection){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while(true){
                String line = reader.readLine();
                if(line == null) break;
                System.out.println(line);
            }
        } catch (Exception e) {
            System.out.println("An Error has occured while trying to read text document: " + e.getMessage());
        }
        
    }

    public static void handleImage(URLConnection connection, String imageType){
        Path img = Paths.get("downloadedImage." + imageType);
        int fileSize = connection.getContentLength();
        System.out.println("Image size: " + fileSize + "bytes");
        System.out.println("Image type: " + imageType);
        byte[] bytes = new byte[1024];
        try(InputStream inputStream = connection.getInputStream(); 
            FileOutputStream fileOutput = new FileOutputStream(img.toFile())) {
            int bytesRead;
            int bytesSavedToFile = 0;
            while((bytesRead = inputStream.read(bytes)) != -1){
                for(int i = 0; i < bytesRead; i++){
                    fileOutput.write(bytes[i]);
                    bytesSavedToFile++;
                    printProgress(bytesSavedToFile, fileSize);                
                }
            }            
            System.out.println("Image downloaded: " + img.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("An Error has occured while trying to read image: " + e.getMessage());
        }
        
    }

    private static int totalNumberOfBars = 20;

    public static void printProgress(int current, int total){
        int percentage = (current*100)/total;
        int currentNumberOfBars = (current*totalNumberOfBars)/total;
        StringBuilder msg = new StringBuilder().append("[");
        for(int i = 1; i <= totalNumberOfBars; i++){
            if(i <= currentNumberOfBars){
                msg.append("#");
            }else msg.append("_");
        }
        msg.append("]").append(" " + percentage + "% ").append(current + "/" + total + " bytes");
        println(msg.toString());
    }

    private static int lastLineLength = 0;

    private static void println(String message){
        System.out.print("\r " + message);
        int currentLineLength = "\r ".length() + message.length();
        for (int i = currentLineLength; i < lastLineLength; i++) {
            System.out.print(" ");
        }
        lastLineLength = currentLineLength;
    }
    
}
