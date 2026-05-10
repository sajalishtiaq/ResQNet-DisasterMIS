package com.disastermis.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                SceneManager.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle(title + " — Disaster MIS");
            primaryStage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T loadController(String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            SceneManager.class.getResource("/fxml/" + fxmlFile));
        loader.load();
        return loader.getController();
    }
}
