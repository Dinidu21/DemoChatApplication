package com.dinidu.demochatapp;

import com.dinidu.demochatapp.controller.ClientController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    private static int clientCounter = 0;
    private static final int MAX_AUTO_CLIENTS = 3;

    @Override
    public void start(Stage primaryStage) throws IOException {
        openClientWindow(primaryStage, "Client " + (++clientCounter));
    }

    public static void openClientWindow(Stage stage, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/client.fxml"));
        Parent root = loader.load();

        ClientController controller = loader.getController();
        controller.setOnSuccessfulConnect(() -> {
            // When this client connects successfully, open another window if we haven't reached the limit
            if (clientCounter < MAX_AUTO_CLIENTS) {
                Platform.runLater(() -> {
                    try {
                        Stage newStage = new Stage();
                        openClientWindow(newStage, "Client " + (++clientCounter));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        stage.setTitle(title);
        stage.setScene(new Scene(root, 600, 450));
        stage.show();

        // Position windows in a cascade pattern
        stage.setX(150 + (clientCounter - 1) * 50);
        stage.setY(100 + (clientCounter - 1) * 50);

        stage.setOnCloseRequest(event -> {
            // If this is the last window, exit the application
            if (--clientCounter == 0) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}