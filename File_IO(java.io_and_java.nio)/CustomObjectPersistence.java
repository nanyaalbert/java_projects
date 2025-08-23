import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class CustomObjectPersistence {
    private static boolean isSerialized = false;
    public static void main(String[] args){
        ArrayList<Product> productList = new ArrayList<>();
        productList.add(new Product(1, "Laptop", 950, 50));
        productList.add(new Product(2, "Phone", 300, 20));

        System.out.println("Attempting to serialize the following products:");
        for (Product product : productList) {
            System.out.println(product);
        }
        serializeProducts(productList);

        if(isSerialized){
            System.out.println("");
            ArrayList<Product> deserializedProducts = deserializeProducts(new File("productlist.bin"));
            if (deserializedProducts != null) {
                for (Product product : deserializedProducts) {
                    System.out.println(product);
                }
            }
        }
    }

    public static class Product implements Serializable{
        private int id;
        private String name;
        private double price;
        private int quantity;

        public Product(int id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String toString(){
            return new StringBuilder("Product{")
                    .append("\nid= ").append(id)
                    .append("\nname= ").append(name)
                    .append("\nprice= ").append(price)
                    .append("\nquantity= ").append(quantity)
                    .append("\n}").append("\n")
                    .toString();
        }
    }

    private static void serializeProducts(ArrayList<Product> products){
        try (ObjectOutputStream productListStream = new ObjectOutputStream(new FileOutputStream("productlist.bin"))) {
            System.out.println("Serializing product list...");
            productListStream.writeObject(products);
            System.out.println("Product list was serialized successfully");
            isSerialized = true;
        } catch (IOException ex) {
            System.out.println("Error serializing product list: " + ex.getMessage());
            ex.printStackTrace();
            isSerialized = false;
        }
    }

    private static ArrayList<Product> deserializeProducts(File file){
        try (ObjectInputStream productListStream = new ObjectInputStream(new FileInputStream(file))) {
            System.out.println("Attempting to deserialize product list...");
            
            try {
                ArrayList<Product> productList = new ArrayList<>();
                productList = (ArrayList<Product>) productListStream.readObject();
                System.out.println("Product list was deserialized successfully");
                return productList;
            } catch (ClassNotFoundException ex) {
                System.out.println("Class not found during deserialization: " + ex.getMessage());
                return null;
            }
           
        } catch (IOException ex) {
            System.out.println("Error deserializing product list: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

}
