import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class BinaryDataProcessor {

    public static void main(String[] args){
        double[] data = new double[10];
        Random rand = new Random();
        for(int i = 0; i < data.length; ++i){
            data[i] = rand.nextDouble(10.0) + 1.0;
        }

        System.out.println("The following random numbers were generated");
        for(int i = 0; i < data.length; ++i){
            System.out.println(data[i] + " ");
        }

        System.out.println();
        System.out.println("Attempting to write the random numbers to a binary file(.bin) on the disk...");
        try (DataOutputStream stream = new DataOutputStream(new FileOutputStream("data.bin"))) {  
             for(int i = 0; i < data.length; ++i){
                stream.writeDouble(data[i]);
            }
            System.out.println("The numbers were successfully written to the binary file");        
        } catch (IOException ex) {
            System.out.println("An error occured while writing to file" + ex.getMessage());
        }

        System.out.println();
        System.out.println("Attempting to read the random numbers from a binary file(.bin) on the disk...");
        System.out.println("Each random number is multiplied by 2.0 after it is read");
        try (DataInputStream stream = new DataInputStream(new FileInputStream("data.bin"))) {  
            while(true){
                double number = stream.readDouble();
                System.out.println(number * 2.0);
            }       
        } catch(EOFException ex){

        } catch (IOException ex) {
            System.out.println("An error occured while reading from file" + ex.getMessage());
        }
    }

}
