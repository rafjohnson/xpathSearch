package dta.xpathsearch;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    // fields
    private static TextField txtDirectory;
    private static TextField txtXpath;
    private static ListView<String> lstFiles;

    @Override
    public void start(Stage stage) {

        // add the grid
        BorderPane bPane = new BorderPane();

        /******* side bar ********/
        // Side bar has a single button. Is a different color than the top bar.
        VBox sideBar = new VBox(30);
        sideBar.setBackground(new Background(new BackgroundFill(Color.LIGHTSEAGREEN, null, null)));
        sideBar.setAlignment(Pos.CENTER);
        sideBar.setPadding(new Insets(10, 10, 10, 10));

        // add to the border pane
        bPane.setRight(sideBar);

        // add the button to the side bar.
        Button btnSearch = new Button("Search");
        btnSearch.setOnAction(event -> {
            btnSearch_Click();
        });

        sideBar.getChildren().add(btnSearch);

        ColumnConstraints lblCol = new ColumnConstraints();
        // lblCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints boxCol = new ColumnConstraints();
        boxCol.setHgrow(Priority.ALWAYS);
        // Middle bar has all the stuff
        GridPane centerArea = new GridPane();
        centerArea.setBackground(new Background(new BackgroundFill(Color.LEMONCHIFFON, null, null)));
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setHgap(10);
        centerArea.setVgap(10);
        centerArea.getColumnConstraints().addAll(new ColumnConstraints(20), lblCol, boxCol, new ColumnConstraints(20));
        // centerArea.setPadding(new Insets(10, 10, 10, 10));
        // add to the border pane in the center view
        bPane.setCenter(centerArea);

        // add the things:
        centerArea.add(new Label("Directory: "), 1, 0);
        centerArea.add(new Label("Xpath:"), 1, 1);
        centerArea.add(new Label("Files: "), 1, 2);

        // text boxes:
        txtDirectory = new TextField();
        txtXpath = new TextField();

        centerArea.add(txtDirectory, 2, 0);
        centerArea.add(txtXpath, 2, 1);

        // listview:
        lstFiles = new ListView<>();
        lstFiles.setPrefHeight(300);
        lstFiles.setOnMouseClicked(event -> {
            lstFiles_Click(event);
        });

        centerArea.add(lstFiles, 2, 2);

        stage.setTitle("XPath Search");
        var scene = new Scene(bPane, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void lstFiles_Click(MouseEvent event) {
        if (event.getClickCount() == 2) {
            // get the selected file
            String file = lstFiles.getSelectionModel().getSelectedItem();
            openFile(file);
        }
    }

    public static void openFile(String filePath) {
        // Create a File object from the file path
        File fileToOpen = new File(filePath);

        // Check if the file exists and can be opened
        if (fileToOpen.exists()) {
            try {
                // Open the file using the default system handler
                Desktop.getDesktop().open(fileToOpen);
            } catch (IOException e) {
                System.err.println("Error opening the file: " + e.getMessage());
            }
        } else {
            System.err.println("The specified file does not exist.");
        }
    }

    public static void btnSearch_Click() {
        // sets the list up, handles the button click
        // get the list of files.
        String tXpath = txtXpath.getText();
        String tDir = txtDirectory.getText();
        List<String> files = new ArrayList<>();

        try {
            files = getListOfFilesInDirectoryWithXpath(tDir, tXpath);
        } catch (Exception e) {
        }

        for (int i = 0; i < files.size(); i++) {
            lstFiles.getItems().add(files.get(i));

        }
    }

    public static boolean isXML(Path file) {
        List<String> XMLExtensions = Arrays.asList("xml", "xslt", "xsl");
        String fileExtension = file.toString().substring(file.toString().lastIndexOf(".") + 1);
        if (XMLExtensions.contains(fileExtension)) {
            return true;
        } else {
            return false;
        }

    }

    public static List<String> getListOfFilesInDirectoryWithXpath(String path, String xPath) throws IOException {
        // https://www.baeldung.com/java-list-directory-files
        List<String> fileList = new ArrayList<String>();
        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file) && isXML(file)) {
                    // check the file for the xpath. If true, just add it to the list.
                    // open/build the document:
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder;

                    try {
                        builder = factory.newDocumentBuilder();
                        Document document = builder.parse(file.toString());
                        // check for xpath.
                        boolean hasXpathValue = false;
                        hasXpathValue = hasXpathValue(document, xPath);
                        if (hasXpathValue) {
                            fileList.add(file.toRealPath().toString());
                        }

                    } catch (SAXException e) {

                    } catch (ParserConfigurationException e) {

                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });

        return fileList;

    }

    public static boolean hasXpathValue(Document document, String xPathExpression)
            throws SAXException, IOException, ParserConfigurationException {
        /// searches a file for an xpath.
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        List<String> values = new ArrayList<>();

        try {
            XPathExpression expr = xpath.compile(xPathExpression);
            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                values.add(nodes.item(i).getNodeValue());
            }

        } catch (XPathExpressionException e) {

        }

        if (values.size() > 0) {
            return true;
        } else {
            return false;
        }

    }
}