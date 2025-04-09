package com.dinidu.demochatapp.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class ServerController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button exitButton;
    @FXML private Label statusLabel;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private volatile boolean running = true;

    public void initialize() {
        // Start server in a separate thread to not block the UI
        new Thread(this::startServer).start();

        // Disable send button if text field is empty
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(5000);
            updateStatus("Server started on port 5000. Waiting for clients...");

            clientSocket = serverSocket.accept();

            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());

            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            updateStatus("Client connected: " + clientAddress);
            appendMessage("System", "Client has joined the chat");

            // Start listening for messages
            receiveMessages();

        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                updateStatus("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void receiveMessages() {
        new Thread(() -> {
            try {
                while (running) {
                    String type = inputStream.readUTF(); // Could be "TEXT" or "IMAGE"

                    if (type.equals("TEXT")) {
                        String message = inputStream.readUTF();
                        if (message.equalsIgnoreCase("exit")) {
                            Platform.runLater(() -> {
                                appendMessage("System", "Client has left the chat");
                                updateStatus("Client disconnected");
                            });
                            break;
                        }
                        Platform.runLater(() -> appendMessage("Client", message));

                    } else if (type.equals("IMAGE")) {
                        String fileName = inputStream.readUTF();
                        int size = inputStream.readInt();

                        byte[] imageBytes = new byte[size];
                        inputStream.readFully(imageBytes);

                        // Save image to disk
                        File imageFile = new File("received-images/" + fileName);
                        imageFile.getParentFile().mkdirs(); // Ensure directory exists
                        Files.write(imageFile.toPath(), imageBytes);

                        Platform.runLater(() -> appendMessage("Client", "[Image received: " + fileName + "]"));
                    }
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> updateStatus("Connection lost: " + e.getMessage()));
                }
            }
        }).start();
    }


    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && clientSocket != null && clientSocket.isConnected()) {
            try {
                outputStream.writeUTF(message);
                outputStream.flush();
                appendMessage("You", message);
                messageField.clear();

                if (message.equalsIgnoreCase("exit")) {
                    closeConnection();
                }
            } catch (IOException e) {
                updateStatus("Failed to send message: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        try {
            if (outputStream != null) {
                outputStream.writeUTF("exit");
                outputStream.flush();
            }
            closeConnection();

            // Close the window
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        running = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String sender, String message) {
        chatArea.appendText(sender + ": " + message + "\n");
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
}