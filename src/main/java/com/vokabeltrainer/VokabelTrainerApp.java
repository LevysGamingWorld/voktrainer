package com.vokabeltrainer;

import com.vokabeltrainer.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * JavaFX-Application – erstellt das Hauptfenster und die Szene.
 */
public class VokabelTrainerApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController();

        Scene scene = new Scene(controller.getRoot(), 1100, 720);
        scene.setFill(Color.web("#0d0d0d"));
        scene.getStylesheets().add(
            getClass().getResource("/styles/style.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("VokabelTrainer");
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        stage.show();
    }
}
