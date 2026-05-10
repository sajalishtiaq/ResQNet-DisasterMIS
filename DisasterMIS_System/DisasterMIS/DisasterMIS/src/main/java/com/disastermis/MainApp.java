package com.disastermis;

import com.disastermis.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);
        primaryStage.setTitle("Disaster MIS — Login");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        SceneManager.switchTo("Login.fxml", "Login");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
