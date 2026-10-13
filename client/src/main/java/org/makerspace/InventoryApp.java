package org.makerspace;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class InventoryApp extends Application {
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(
            new Label("Makerspace Inventory")
        );

        stage.setTitle("Makerspace Inventory");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}