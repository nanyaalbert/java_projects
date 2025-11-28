import java.io.File;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLDigitalRecipeBookUsingDOM {
    private static DocumentBuilderFactory factory;
    private static DocumentBuilder builder;
    private static Document document;
    private static final String recipeFilePath = "recipebook.xml";
    
    public static void main(String[] args){
        InitialStartUp();
    }

    private static void InitialStartUp(){
        System.out.println("welcome to the XML digital recipe book using DOM");
        if(loadRecipeDocument(recipeFilePath)){
            System.out.println("recipe book is ready for use.");
            System.out.println("you can now add, edit, delete and view recipes");
            homeMenu();
        } else return;

    }

    private static void homeMenu(){
        System.out.println("please select an option by entering the corresponding number:");
        System.out.println(" 1. add new recipe");
        System.out.println(" 2. edit existing recipe");
        System.out.println(" 3. delete a recipe");
        System.out.println(" 4. view all recipes");
        System.out.println(" 5. exit application");
        System.out.println("Enter choice (1-5):");

        addNewRecipe();
    }

    private static boolean loadRecipeDocument(String filePath){
        File recipeFile = new File(filePath);
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            System.out.println("error initializing XML parser: " + e.getMessage());
        }
        if(!recipeFile.exists()){
            System.out.println("recipe book does not exist, a new and empty one will be created");
            try {
                document = builder.newDocument();
                Element rootElement = document.createElement("recipebook");
                document.appendChild(rootElement);
                recipeFile.createNewFile();
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{[http://xml.apache.org/xslt](http://xml.apache.org/xslt)}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(recipeFile);
                transformer.transform(source, result);
                System.out.println("new and empty recipe book file created successfully");
                return true;
            } catch (Exception e) {
                System.out.println("error occured while creating a new recipe book file: " + e.getMessage());
                return false;
            }
        } else {
            try {
                document = builder.parse(recipeFile);
            } catch (Exception e) {
                System.out.println("error occured while loading existing recipe book: " + e.getMessage());
            }
            if(verifyXMLStructure(document)){
                System.out.println("recipe book loaded successfully.");
                return true;
            } else{
                System.out.println("invalid recipe book structure");
                return false;
            }

        }      

    }

    private static boolean verifyXMLStructure(Document document){
        Element rootElement = document.getDocumentElement();
        if(rootElement.getTagName().equals("recipebook")){
            return true;
        }else{
            return false;
        }
    }

    private static void addNewRecipe(){
        StringBuilder instructions = new StringBuilder();
        instructions.append("---Let's Add a New Recipe---");
        instructions.append("\nBefore we begin, it's important to understand how your recipe will be organized.");
        instructions.append(" Recipes in this system are structured to support complex dishes");
        instructions.append(" that have multiple parts (like a main course with a side dish or sauce).\n");
        instructions.append("\nEach Recipe is composed of three levels:\n");
        instructions.append("\n1. The Recipe (Title & Description):");
        instructions.append("\n\t- You'll start with a general Title (e.g., \"Ofada Rice and Ayamase Stew\").");
        instructions.append("\n\t- A brief Description of the whole dish.");
        instructions.append("\n2. Components (The Parts of the Dish):");
        instructions.append("\n\t- Every recipe must have at least one Component.");
        instructions.append(" This separates the main dish from its sides or sauces");
        instructions.append(" (e.g., Component 1: \"Ofada Rice,\" Component 2: \"Ayamase Stew\").");
        instructions.append("\n\t- For each component, you will enter its specific details.");
        instructions.append("\n3. Ingredients and Preparation:");
        instructions.append("\n\t- For each component, we will collect a list of Ingredients (including the name and quantity).");
        instructions.append("\n\t- We will then collect the Preparation Instructions as a sequence of numbered steps for that specific component.\n");
        instructions.append("\nWe will go through this structure step-by-step.\n");
        instructions.append("\nPress Enter to continue...");
        System.out.println(instructions.toString());
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        Element rootElement = document.getDocumentElement();
        int recipePosition = rootElement.getElementsByTagName("recipe").getLength() + 1;
        Element newRecipeElement = recipePositionAndIDUpdater(document.createElement("recipe"), recipePosition);

        System.out.println("Enter the recipe title:");
        String recipeTitle = scanner.nextLine();
        Element titleElement = document.createElement("title");
        titleElement.setTextContent(recipeTitle);
        newRecipeElement.appendChild(titleElement);

        System.out.println("Enter the recipe description:");
        String recipeDescription = scanner.nextLine();
        Element descriptionElement = document.createElement("description");
        descriptionElement.setTextContent(recipeDescription);
        newRecipeElement.appendChild(descriptionElement);

        int numberOfComponents;
        while(true){
            System.out.println("\nEnter the number of components in this recipe (at least 1):");
            try {
                numberOfComponents = Integer.parseInt(scanner.nextLine());
                break;
            } catch (Exception e) {
                System.out.println("Invalid input: Please enter a valid number.");
            }
        }

        for(int i = 0; i < numberOfComponents; i++){
            System.out.println("\nEnter details for component " + (i + 1));
            Element newComponentElement = componentPositionAndIDUpdater(document.createElement("component"), i + 1, newRecipeElement.getAttribute("id"));
            System.out.println("Enter the component name:");
            Element newComponentNameElement = document.createElement("name");
            newComponentNameElement.setTextContent(scanner.nextLine());
            newComponentElement.appendChild(newComponentNameElement);
            newRecipeElement.appendChild(newComponentElement);

            System.out.println("\nNow let's add ingredients for this component.");
            System.out.println("Press [enter] after each filling the name and quantity for each ingredient. type done and press [enter] when finished.");
            int ingredientPosition = 0;
            while(true){
                System.out.println("Enter the ingredient name");
                String ingredientName = scanner.nextLine();
                if(ingredientName.equalsIgnoreCase("done")){
                    break;
                }
                System.out.println("Enter the ingredient quantity (e.g., 2 cups, 1 tablespoon, etc.):");
                String ingredientQuantity = scanner.nextLine();
                ingredientPosition++;
                Element newIngredientElement = ingredientPositionAndIDUpdater(document.createElement("ingredient"), ingredientPosition, newComponentElement.getAttribute("id"));
                Element ingredientNameElement = document.createElement("name");
                ingredientNameElement.setTextContent(ingredientName);
                newIngredientElement.appendChild(ingredientNameElement);
                Element ingredientQuantityElement = document.createElement("quantity");
                ingredientQuantityElement.setTextContent(ingredientQuantity);
                newIngredientElement.appendChild(ingredientQuantityElement);
                newComponentElement.appendChild(newIngredientElement);
            }

            System.out.println("\nNow let's add preparation instructions for this component.");
            System.out.println("Press [enter] after each instruction step. type done and press [enter] when finished.");
            int instructionPosition = 0;
            while(true){
                System.out.println("Enter instruction step");
                String instructionStep = scanner.nextLine();
                if(instructionStep.equalsIgnoreCase("done")){
                    break;
                }
                instructionPosition++;
                Element newInstructionElement = instructionPositionAndIDUpdater(document.createElement("instruction"), instructionPosition, newComponentElement.getAttribute("id"));
                newInstructionElement.setTextContent(instructionStep);
                newComponentElement.appendChild(newInstructionElement);
            }
        }
        rootElement.appendChild(newRecipeElement);

        saveToFile(recipeFilePath);
        scanner.close();
    }

    private static Element recipePositionAndIDUpdater(Element recipeElement, int position){
        recipeElement.setAttribute("position", Integer.toString(position));
        recipeElement.setAttribute("id", "r" + position);
        return recipeElement;
    }

    private static Element componentPositionAndIDUpdater(Element componentElement, int position, String recipeElementID){
        componentElement.setAttribute("position", Integer.toString(position));
        componentElement.setAttribute("id", recipeElementID + "c" + position);
        return componentElement;
    }

    private static Element ingredientPositionAndIDUpdater(Element ingredientElement, int position, String componentElementID){
        ingredientElement.setAttribute("position", Integer.toString(position));
        ingredientElement.setAttribute("id", componentElementID + "i" + position);
        return ingredientElement;
    }

    private static Element instructionPositionAndIDUpdater(Element instructionElement, int position, String componentElementID){
        instructionElement.setAttribute("position", Integer.toString(position));
        instructionElement.setAttribute("id", componentElementID + "s" + position);
        return instructionElement;
    }

    private static void saveToFile(String filePath){
        try {
            File recipeFile = new File(filePath);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{[http://xml.apache.org/xslt](http://xml.apache.org/xslt)}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(recipeFile);
            transformer.transform(source, result);
        } catch (Exception e) {
            System.out.println("error occured while saving recipe: " + e.getMessage());
        }
    }

}
