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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        System.out.println("\t1. add new recipe");
        System.out.println("\t2. manage recipes");
        System.out.println("\t3. exit application");
        System.out.println("Enter choice (1-3):");

        //addNewRecipe();
        manageRecipes(document);
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

        saveToFile();
        scanner.close();
    }
  
    private static void manageRecipes(Document document){
        Element rootElement = document.getDocumentElement();
        System.out.println("Here are the recipes in your recipe book");
        System.out.println("-----------------------------------------");
        System.out.println("ID\tRecipe Title");
        System.out.println("-----------------------------------------");
        NodeList recipeList = rootElement.getElementsByTagName("recipe");
        for(int i = 0; i < recipeList.getLength(); i++){
            Element recipeElement = (Element) recipeList.item(i);
            System.out.println(recipeElement.getAttribute("id") + "\t" + recipeElement.getElementsByTagName("title").item(0).getTextContent());
        }
        System.out.println("-----------------------------------------\n");
        System.out.println("please select an option by entering the corresponding number:");
        System.out.println("\t1. view recipe");
        System.out.println("\t2. edit recipes");
        System.out.println("\t3. delete recipe");
        System.out.println("Enter choice (1-3) or type 'home' to return to main menu");
        Scanner scanner = new Scanner(System.in);
        outerLoop:
        while(true){
            switch(scanner.nextLine()){
                case "1" -> {
                    innerLoop:
                    while(true){
                        System.out.println("Enter the recipe ID to view:");
                        String recipeID = scanner.nextLine();
                        for(int i = 0; i < recipeList.getLength(); i++){
                            Element recipeElement = (Element) recipeList.item(i);
                            if(recipeElement.getAttribute("id").equalsIgnoreCase(recipeID)){
                                viewSingleRecipe(recipeElement);
                                break outerLoop;
                            }
                        }
                        System.out.println("We couldn't find a recipe with that ID. Please try again.");
                        continue innerLoop;
                    }
                }
                case "2" -> {}
                case "3" -> {
                    int deletedRecipePosition = -1;
                    int boundaryElementPosition = -1;
                    innerLoop:
                    while(true){
                        System.out.println("Enter the recipe ID to delete:");
                        String recipeID = scanner.nextLine();
                        for(int i = 0; i < recipeList.getLength(); i++){
                            Element recipeElement = (Element) recipeList.item(i);                                                      

                            if(recipeElement.getAttribute("id").equalsIgnoreCase(recipeID)){
                                deletedRecipePosition = Integer.parseInt(recipeElement.getAttribute("position"));
                                if(recipeList.getLength() > 1 && deletedRecipePosition != 1){
                                    boundaryElementPosition = deletedRecipePosition - 1;
                                } else if(recipeList.getLength() > 1 && deletedRecipePosition != recipeList.getLength()){
                                    boundaryElementPosition = deletedRecipePosition - 1;
                                } else if(recipeList.getLength() > 1 && deletedRecipePosition == 1){
                                    boundaryElementPosition = deletedRecipePosition;
                                }
                                rootElement.removeChild(recipeElement);
                                saveToFile();
                                System.out.println("Recipe with ID " + recipeID + " has been deleted.");
                                break innerLoop;
                            }                            
                        }
                        System.out.println("We couldn't find a recipe with that ID. Please try again.");
                        continue innerLoop;
                    }
                    if(boundaryElementPosition != -1){
                        for(int j = boundaryElementPosition; j < recipeList.getLength(); j++){
                            Element recipeElementForPositionUpdate = (Element) recipeList.item(j);
                            recipeElementForPositionUpdate = (Element) recipePositionAndIDUpdater(recipeElementForPositionUpdate, (j + 1));
                            NodeList recipeChildList = recipeElementForPositionUpdate.getChildNodes();
                            for(int k = 0; k < recipeChildList.getLength(); k++){
                                if(recipeChildList.item(k).getNodeType() == Node.ELEMENT_NODE){
                                    Element recipeChildElement = (Element) recipeChildList.item(k);
                                    if(recipeChildElement.getTagName().equals("component")){
                                        Element componentElementForPositionUpdate = (Element) componentPositionAndIDUpdater(recipeChildElement, Integer.parseInt(recipeChildElement.getAttribute("position")), recipeElementForPositionUpdate.getAttribute("id"));
                                        NodeList componentChildList = recipeChildElement.getChildNodes();
                                        for(int l = 0; l < componentChildList.getLength(); l++){
                                            if(componentChildList.item(l).getNodeType() == Node.ELEMENT_NODE){
                                                Element componentChildElement = (Element) componentChildList.item(l);
                                                if(componentChildElement.getTagName().equals("ingredient")){
                                                    componentChildElement = (Element) ingredientPositionAndIDUpdater(componentChildElement, Integer.parseInt(componentChildElement.getAttribute("position")), componentElementForPositionUpdate.getAttribute("id"));
                                                } else if(componentChildElement.getTagName().equals("instruction")){
                                                    componentChildElement = (Element) instructionPositionAndIDUpdater(componentChildElement, Integer.parseInt(componentChildElement.getAttribute("position")), componentElementForPositionUpdate.getAttribute("id"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    saveToFile();
                    break outerLoop;
                    
                }
                case "home" -> homeMenu();
                default -> {
                    System.out.println("invalid choice, returning to main menu");
                    homeMenu();
                }
            }
        }
        scanner.close();
    }

    private static void viewSingleRecipe(Element recipeElement){
        System.out.println("Recipe title: \n" + recipeElement.getElementsByTagName("title").item(0).getTextContent());
        System.out.println("Recipe description: \n" + recipeElement.getElementsByTagName("description").item(0).getTextContent());
        NodeList components = recipeElement.getElementsByTagName("component");
        System.out.println();
        System.out.println("This recipe has " + components.getLength() + " components.");
        for(int i = 0; i < components.getLength(); i++){
            Element componentElement = (Element) components.item(i);
            System.out.println("Component " + (i + 1) + " name: " + componentElement.getElementsByTagName("name").item(0).getTextContent());
            NodeList ingredients = componentElement.getElementsByTagName("ingredient");
            System.out.println("This component has " + ingredients.getLength() + " ingredients:");
            for(int j = 0; j < ingredients.getLength(); j++){
                Element ingredientElement = (Element) ingredients.item(j);
                System.out.println("\tIngredient " + (j + 1) + " name: " + ingredientElement.getElementsByTagName("name").item(0).getTextContent());
                System.out.println("\tIngredient " + (j + 1) + " quantity: " + ingredientElement.getElementsByTagName("quantity").item(0).getTextContent());
            }
            NodeList instructions = componentElement.getElementsByTagName("instruction");
            System.out.println("This component has " + instructions.getLength() + " preparation steps:");
            for(int k = 0; k < instructions.getLength(); k++){
                Element instructionElement = (Element) instructions.item(k);
                System.out.println("\tStep " + (k + 1) + ": " + instructionElement.getTextContent());
            }
            System.out.println();
        }
    }

    private static void exitApplication(){
        System.out.println("Exiting application...");
        System.exit(0);
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

    private static void saveToFile(){
        try {
            File recipeFile = new File(recipeFilePath);
            removeWhiteSpaceNodes(document.getDocumentElement());
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

    private static void removeWhiteSpaceNodes(Node node){
        NodeList childNodes = node.getChildNodes();
        for(int i = childNodes.getLength() - 1; i >= 0; i--){
            if((childNodes.item(i).getNodeType() == Node.TEXT_NODE) && childNodes.item(i).getNodeValue().trim().isEmpty()){
                childNodes.item(i).getParentNode().removeChild(childNodes.item(i));
            } else if(childNodes.item(i).getNodeType() == Node.ELEMENT_NODE){
                removeWhiteSpaceNodes(childNodes.item(i));
            }

        }
    }

}
