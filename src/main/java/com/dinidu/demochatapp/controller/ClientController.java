package com.dinidu.demochatapp.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private TextField usernameField;
    @FXML private Button sendButton;
    @FXML private Button exitButton;
    @FXML private Button connectButton;
    @FXML private Label connectionStatus;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private volatile boolean running = false;
    private String username;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // Callback for successful connection
    private Runnable onSuccessfulConnect;

    public void initialize() {
        // Set a default username based on window title (will be set when window opens)
        Platform.runLater(() -> {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            usernameField.setText(stage.getTitle());
        });

        // Disable send button if text field is empty
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
        connectButton.disableProperty().bind(usernameField.textProperty().isEmpty());

    }

    public void setOnSuccessfulConnect(Runnable callback) {
        this.onSuccessfulConnect = callback;
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        if (running) {
            return;
        }

        username = usernameField.getText().trim();
        if (username.isEmpty()) {
            appendMessage("System: Please enter a username");
            return;
        }

        // Update window title with username
        Platform.runLater(() -> {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle(username);
        });

/*        // Disable username field and connect button
        usernameField.setDisable(true);
        connectButton.setDisable(true);*/

        // Connect to server in a separate thread
        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            updateStatus("Connecting to server...");
            socket = new Socket("localhost", 5000);

            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Send username as first message
            outputStream.writeUTF(username);
            outputStream.flush();

            running = true;
            updateStatus("Connected as: " + username);
            appendMessage("System: Connected to the chat server!");

            // Enable message controls
            Platform.runLater(() -> {
                // Unbind and then enable buttons
                messageField.setDisable(false);
                sendButton.disableProperty().unbind();  // Unbind the button disable property
                sendButton.setDisable(false);
                exitButton.setDisable(false);

                // Call the callback to notify about successful connection
                if (onSuccessfulConnect != null) {
                    onSuccessfulConnect.run();
                }
            });

            // Start listening for messages
            receiveMessages();

        } catch (IOException e) {
            updateStatus("Failed to connect: " + e.getMessage());
            appendMessage("System: Connection failed - " + e.getMessage());

            // Re-enable connection controls
            Platform.runLater(() -> {
                usernameField.setDisable(false);
                connectButton.setDisable(false);
            });
        }
    }


    private void receiveMessages() {
        new Thread(() -> {
            try {
                while (running) {
                    String message = inputStream.readUTF();
                    if (message.equalsIgnoreCase("exit")) {
                        Platform.runLater(() -> {
                            appendMessage("System: Server has closed the connection");
                            updateStatus("Disconnected from server");
                            handleDisconnect();
                        });
                        break;
                    }
                    Platform.runLater(() -> appendMessage(message));
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> {
                        updateStatus("Connection lost: " + e.getMessage());
                        appendMessage("System: Connection lost - " + e.getMessage());
                        handleDisconnect();
                    });
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
                messageField.clear();
            } catch (IOException e) {
                appendMessage("System: Failed to send message - " + e.getMessage());
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
            handleDisconnect();

            if (((Button)event.getSource()).getText().equals("Close")) {
                // Close the window if explicitly requested
                Stage stage = (Stage) exitButton.getScene().getWindow();
                stage.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDisconnect() {
        running = false;
        closeConnection();

        // Reset UI
        Platform.runLater(() -> {
            usernameField.setDisable(false);
            connectButton.setDisable(false);
            messageField.setDisable(true);
            sendButton.setDisable(true);
            exitButton.setText("Close");
        });
    }

    private void closeConnection() {
        try {
            running = false;
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String message) {
        String timestamp = timeFormat.format(new Date());
        chatArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> connectionStatus.setText(status));
    }
}