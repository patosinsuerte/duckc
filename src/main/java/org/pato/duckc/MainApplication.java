package org.pato.duckc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.pato.duckc.controllers.MainController;
import org.pato.duckc.services.FileProcessorService;
import org.pato.duckc.services.ImageCompressionService;
import org.pato.duckc.services.UserPreferencesService;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        UserPreferencesService userPreferencesService = new UserPreferencesService();
        FileProcessorService fileProcessorService = new FileProcessorService();
        ImageCompressionService imageCompressionService = new ImageCompressionService();
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("fxml/main-view.fxml"));


        fxmlLoader.setControllerFactory(controllerClass -> {
            if (controllerClass == MainController.class) {
                return new MainController(userPreferencesService, fileProcessorService, imageCompressionService);
            }
            try {
                return controllerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        Scene scene = new Scene(fxmlLoader.load(), 300, 400);
        stage.setTitle("DuckC");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();


        MainController controller = fxmlLoader.getController();

        stage.setOnCloseRequest(event -> {
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }
}
