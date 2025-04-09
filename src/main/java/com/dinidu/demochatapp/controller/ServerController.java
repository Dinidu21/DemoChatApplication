package com.dinidu.demochatapp.controller;

import com.dinidu.demochatapp.ClientHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button stopServerButton;
    @FXML private Label statusLabel;
    @FXML private ListView<String> clientListView;
    @FXML private Label clientCountLabel;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ObservableList<String> clientList = FXCollections.observableArrayList();
    private ExecutorService executor;
    private volatile boolean running = true;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public void initialize() {
        // Initialize client list
        clientListView.setItems(clientList);

        // Disable send button if text field is empty
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());

        // Start server in a separate thread
        executor = Executors.newCachedThreadPool();
        executor.submit(this::startServer);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(5000);
            updateStatus("Server started on port 5000");
            appendMessage("Server started and waiting for connections...");

            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            this::appendMessage,
                            this::handleClientMessage,
                            this::handleClientDisconnect
                    );

                    synchronized (clients) {
                        clients.add(handler);
                    }

                    executor.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        appendMessage("Error accepting client connection: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            updateStatus("Failed to start server: " + e.getMessage());
            appendMessage("Server error: " + e.getMessage());
        }
    }

    private void handleClientMessage(String message, ClientHandler sourceClient) {
        String formattedMessage = sourceClient.getClientName() + ": " + message;
        appendMessage(formattedMessage);

        // Broadcast to all clients
        synchronized (clients) {
            for (ClientHandler client : clients) {
                // We could skip sending back to the source client if desired
                client.sendMessage(formattedMessage);
            }
        }
    }

    private void handleClientDisconnect(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
            Platform.runLater(() -> {
                clientList.remove(client.getClientName() + " (" + client.getClientAddress() + ")");
                updateClientCount();
            });
        }
    }

    private void updateClientCount() {
        Platform.runLater(() -> clientCountLabel.setText("Active clients: " + clients.size()));
    }

    private void broadcastMessage(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String formattedMessage = "Server: " + message;
            broadcastMessage(formattedMessage);
            appendMessage(formattedMessage);
            messageField.clear();
        }
    }

    @FXML
    private void handleStopServer(ActionEvent event) {
        try {
            // Notify clients server is shutting down
            broadcastMessage("exit");

            // Shutdown
            shutdown();

            // Close window
            Stage stage = (Stage) stopServerButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shutdown() {
        running = false;

        // Close all client connections
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.closeConnection();
            }
            clients.clear();
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
        }
    }

    void appendMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = timeFormat.format(new Date());
            chatArea.appendText("[" + timestamp + "] " + message + "\n");

            // Update client list if it's a connection message
            if (message.contains("connected from")) {
                String[] parts = message.split("'");
                if (parts.length >= 2) {
                    String clientName = parts[1];
                    String address = message.substring(message.lastIndexOf(" ") + 1);
                    clientList.add(clientName + " (" + address + ")");
                    updateClientCount();
                }
            }
        });
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
}