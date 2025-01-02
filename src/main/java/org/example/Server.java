package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

public class Server {
    private final int port;
    private final InvertedIndex invertedIndex;
    private final ThreadPool threadPool;

    public Server(int port, int numThreads) {
        this.port = port;
        this.invertedIndex = new InvertedIndex();
        this.threadPool = new ThreadPool(numThreads);
    }

    public void start(String datasetPath) {
        try {
            invertedIndex.loadDocumentsFromDirectory(datasetPath);
            System.out.println("[SERVER] Initial dataset loaded.");

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[SERVER] Server started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[SERVER] New client connected: " + clientSocket.getRemoteSocketAddress());
                    threadPool.submitTask(() -> handleClient(clientSocket));
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error starting the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("[SERVER] Received command: " + command);
                if (command.startsWith("ADD")) {
                    String[] parts = command.split(" ", 3);
                    if (parts.length < 3) {
                        out.println("[ERROR] Invalid ADD command format.");
                        System.out.println("[SERVER] Invalid ADD command received.");
                        continue;
                    }

                    String docId = parts[1];
                    String content = parts[2];

                    // Save new file to directory
                    String filePath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset\\" + docId + ".txt";
                    Files.writeString(Paths.get(filePath), content);

                    invertedIndex.addDocument(docId, content);
                    out.println("[SERVER] Document added.");
                    System.out.println("[SERVER] Document added with ID: " + docId);

                } else if (command.startsWith("SEARCH")) {
                    String[] parts = command.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("[ERROR] Invalid SEARCH command format.");
                        System.out.println("[SERVER] Invalid SEARCH command received.");
                        continue;
                    }

                    String word = parts[1];
                    Set<String> results = invertedIndex.search(word);
                    if (results.isEmpty()) {
                        out.println("[SERVER] No files found containing the word: " + word);
                        System.out.println("[SERVER] Search result for '" + word + "': No matches found.");
                    } else {
                        out.println("[SERVER] Results: " + results);
                        System.out.println("[SERVER] Search result for '" + word + "': " + results);
                    }
                } else if (command.equals("EXIT")) {
                    out.println("[SERVER] Goodbye!");
                    System.out.println("[SERVER] Client disconnected: " + clientSocket.getRemoteSocketAddress());
                    break;
                } else {
                    out.println("[ERROR] Invalid command.");
                    System.out.println("[SERVER] Invalid command received: " + command);
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("[SERVER] Connection closed: " + clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                System.err.println("[SERVER] Error closing client socket: " + e.getMessage());
            }
        }
    }

    public void stop() {
        threadPool.shutdown();
        System.out.println("[SERVER] Server stopped.");
    }

    public static void main(String[] args) {
        String datasetPath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset";
        Server server = new Server(8080, 4);
        server.start(datasetPath);
    }
}