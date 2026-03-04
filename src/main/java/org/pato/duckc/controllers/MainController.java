package org.pato.duckc.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.pato.duckc.services.FileProcessorService;
import org.pato.duckc.services.ImageCompressionService;
import org.pato.duckc.services.UserPreferencesService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController implements Initializable {

    private final UserPreferencesService userPreferencesService;
    private final FileProcessorService fileProcessorService;
    private final ImageCompressionService imageCompressionService;
    private ExecutorService currentService;
    private Thread masterThread;

    public MainController(UserPreferencesService userPreferencesService, FileProcessorService fileProcessorService, ImageCompressionService imageCompressionService) {
        this.userPreferencesService = userPreferencesService;
        this.fileProcessorService = fileProcessorService;
        this.imageCompressionService = imageCompressionService;
    }


    @FXML
    private VBox overlayPane;

    @FXML
    private void showOverlay() {
        overlayPane.setVisible(true);
    }

    @FXML
    private void hideOverlay() {
        overlayPane.setVisible(false);
    }


    @FXML
    protected Label labelPath;

    @FXML
    private VBox filesContainer;


    @FXML
    private Label dragImageLabel;

    @FXML
    protected void onSelectOutputFolder(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a folder to save the images");

        File initialDir = new File(userPreferencesService.getDestinationPath());
        if (initialDir.exists()) {
            directoryChooser.setInitialDirectory(initialDir);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            userPreferencesService.setDestinationPath(selectedDirectory.getAbsolutePath());
        }
    }


    private void updateFiles() {
        filesContainer.getChildren().clear();

        if (fileProcessorService.getFilesToProcess().isEmpty()) {
            if (dragImageLabel != null) {
                filesContainer.getChildren().add(dragImageLabel);
            }

        }

        for (File file : fileProcessorService.getFilesToProcess()) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(10);
            row.getStyleClass().add("scroll-item");

            Label fileLabel = new Label(file.getName());
            fileLabel.getStyleClass().add("scroll-item__text");


            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);


            Button btnRemove = new Button("X");
            btnRemove.getStyleClass().add("scroll-item__button");
            btnRemove.setStyle("-fx-text-fill: red;");


            btnRemove.setOnAction(e -> {
                fileProcessorService.removeFile(file);
                updateFiles();
            });


            row.getChildren().addAll(fileLabel, spacer, btnRemove);


            filesContainer.getChildren().add(row);
        }
    }


    @FXML
    protected void onClearAllFiles(ActionEvent event) {
        fileProcessorService.clear();
        updateFiles();
    }


    @FXML
    protected void onSelectFiles(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imágenes");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null) {
            int currentFiles = fileProcessorService.getFilesToProcess().size();
            // LÍMITE DE 30
            if (currentFiles + selectedFiles.size() > 30) {
                showLimitAlert();
                return;
            }
            fileProcessorService.addFiles(selectedFiles);
            updateFiles();
        }
    }

    @FXML
    protected void onCompressClick(ActionEvent event) {
        String destination = userPreferencesService.getDestinationPath();

        File destDir = new File(destination);
        if (destination == null || destination.isEmpty() || !destDir.exists() || !destDir.isDirectory()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid destination folder");
            alert.setHeaderText("Quack! I can't find the folder");
            alert.setContentText("The selected folder does not exist or is invalid. Please select one before continuing.");
            alert.showAndWait();
            return;
        }

        List<File> files = new ArrayList<>(fileProcessorService.getFilesToProcess());

        if (files.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Empty List");
            alert.setHeaderText("Quack! nothing to do.");
            alert.setContentText("First, drag or select some images so I can get to work.");
            alert.showAndWait();
            return;
        }


        showOverlay();
        List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());
        currentService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        masterThread = new Thread(() -> {
            CountDownLatch latch = new CountDownLatch(files.size());

            for (File f : files) {
                currentService.submit(() -> {
                    try {
                        if (Thread.currentThread().isInterrupted()) return;
                        imageCompressionService.compress(f, destination, 0.4f);
                    } catch (IOException e) {
                        failedFiles.add(f.getName() + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();

                Platform.runLater(() -> {
                    fileProcessorService.clear();
                    updateFiles();
                    hideOverlay();
                    currentService.shutdown();


                    if (!failedFiles.isEmpty()) {
                        showErrorAlert(failedFiles);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        masterThread.setDaemon(true);
        masterThread.start();
    }


    private void showErrorAlert(List<String> failedFiles) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Compression error.");
        alert.setHeaderText("Quack! Some files could not be processed:");

        StringBuilder sb = new StringBuilder();
        failedFiles.forEach(msg -> sb.append("- ").append(msg).append("\n"));

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(150);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }


    @FXML
    private void onCancelCompress() {
        if (currentService != null) {
            currentService.shutdownNow();
        }

        if (masterThread != null && masterThread.isAlive()) {
            masterThread.interrupt();
        }

        hideOverlay();
//        System.out.println("Operación abortada.");
    }


    private void showLimitAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("File limit reached");
        alert.setHeaderText("Quack! Too many files");
        alert.setContentText("The maximum limit is 30 files to ensure optimal performance.");
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (labelPath != null) {
            labelPath.textProperty().bind(userPreferencesService.destinationPathProperty());
        }
        updateFiles();

    }

    public void shutdown() {
//        System.out.println("Limpiando recursos antes de cerrar...");
        if (currentService != null) {
            currentService.shutdownNow();
        }
        if (masterThread != null && masterThread.isAlive()) {
            masterThread.interrupt();
        }
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            List<File> droppedFiles = db.getFiles();


            int currentFiles = fileProcessorService.getFilesToProcess().size();

            if (currentFiles + droppedFiles.size() > 30) {
                showLimitAlert();
            } else {
                List<File> validImages = droppedFiles.stream()
                        .filter(file -> {
                            String name = file.getName().toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
                        })
                        .toList();

                fileProcessorService.addFiles(validImages);
                updateFiles();
                success = true;
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }


}