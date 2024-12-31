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
            System.out.println("Initial dataset loaded.");

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submitTask(() -> handleClient(clientSocket));
                }
            }
        } catch (IOException e) {
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
                if (command.startsWith("ADD")) {
                    String[] parts = command.split(" ", 3);
                    String docId = parts[1];
                    String content = parts[2];

                    String filePath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset\\" + docId + ".txt";
                    Files.writeString(Paths.get(filePath), content);

                    invertedIndex.addDocument(docId, content);
                    out.println("Document added.");
                } else if (command.startsWith("SEARCH")) {
                    String word = command.split(" ", 2)[1];
                    Set<String> results = invertedIndex.search(word);
                    if (results.isEmpty()) {
                        out.println("No files found containing the word: " + word);
                    } else {
                        out.println("Results: " + results);
                    }
                } else if (command.equals("EXIT")) {
                    out.println("Goodbye!");
                    break;
                } else {
                    out.println("Invalid command.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        threadPool.shutdown();
    }

    public static void main(String[] args) {
        String datasetPath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset";
        Server server = new Server(8080, 4);
        server.start(datasetPath);
    }
}