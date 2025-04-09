package com.dinidu.demochatapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String clientName;
    private final Consumer<String> messageCallback;
    private final BiConsumer<String, ClientHandler> clientMessageCallback;
    private final Consumer<ClientHandler> disconnectCallback;
    private volatile boolean running = true;

    public ClientHandler(Socket socket,
                         Consumer<String> messageCallback,
                         BiConsumer<String, ClientHandler> clientMessageCallback,
                         Consumer<ClientHandler> disconnectCallback) {
        this.socket = socket;
        this.messageCallback = messageCallback;
        this.clientMessageCallback = clientMessageCallback;
        this.disconnectCallback = disconnectCallback;

        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // First message from client should be their name
            clientName = inputStream.readUTF();
            messageCallback.accept("Client '" + clientName + "' connected from " + socket.getInetAddress().getHostAddress());

            // Listen for messages
            while (running) {
                String message = inputStream.readUTF();
                if (message.equalsIgnoreCase("exit")) {
                    messageCallback.accept("Client '" + clientName + "' has left the chat");
                    break;
                }

                // Pass message to server for broadcasting with the source client
                clientMessageCallback.accept(message, this);
            }
        } catch (IOException e) {
            if (running) {
                messageCallback.accept("Connection lost with client '" + clientName + "': " + e.getMessage());
            }
        } finally {
            disconnectCallback.accept(this);
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        try {
            if (socket != null && !socket.isClosed() && outputStream != null) {
                outputStream.writeUTF(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            messageCallback.accept("Failed to send message to '" + clientName + "': " + e.getMessage());
        }
    }

    public void closeConnection() {
        running = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}