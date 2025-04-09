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
import java.io.IOException;
import java.net.Socket;

public class ClientController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button exitButton;
    @FXML private Label connectionStatus;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private volatile boolean running = true;

    public void initialize() {
        // Connect to server in a separate thread to not block the UI
        new Thread(this::connectToServer).start();

        // Disable send button if text field is empty
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
    }

    private void connectToServer() {
        try {
            updateStatus("Connecting to server...");
            socket = new Socket("localhost", 5000);

            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            updateStatus("Connected to server: " + socket.getInetAddress().getHostAddress());
            appendMessage("System", "Connection established!");

            // Start listening for messages
            receiveMessages();

        } catch (IOException e) {
            updateStatus("Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        new Thread(() -> {
            try {
                while (running) {
                    String message = inputStream.readUTF();
                    if (message.equalsIgnoreCase("exit")) {
                        Platform.runLater(() -> {
                            appendMessage("System", "Server has left the chat");
                            updateStatus("Disconnected from server");
                        });
                        break;
                    }
                    Platform.runLater(() -> appendMessage("Server", message));
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
        if (!message.isEmpty() && socket != null && socket.isConnected()) {
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
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String sender, String message) {
        chatArea.appendText(sender + ": " + message + "\n");
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> connectionStatus.setText(status));
    }
}