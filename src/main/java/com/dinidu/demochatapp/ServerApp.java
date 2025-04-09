package com.dinidu.demochatapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ServerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/server.fxml"));
        primaryStage.setTitle("Group Chat Server");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}