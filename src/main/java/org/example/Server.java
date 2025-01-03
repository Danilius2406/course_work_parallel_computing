package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
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

            new Thread(this::startTcpServer).start();

            startHttpServer();

        } catch (IOException e) {
            System.err.println("[SERVER] Error starting the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] TCP Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New TCP client connected: " + clientSocket.getRemoteSocketAddress());
                threadPool.submitTask(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error starting TCP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port + 1), 0);
        httpServer.createContext("/search", new HttpSearchHandler());
        httpServer.setExecutor(threadPool.asExecutor());
        httpServer.start();
        System.out.println("[SERVER] HTTP Server started on port " + (port + 1));
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
                        continue;
                    }

                    String docId = parts[1];
                    String content = parts[2];

                    String filePath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset\\" + docId + ".txt";
                    Files.writeString(Paths.get(filePath), content);

                    invertedIndex.addDocument(docId, content);
                    out.println("[SERVER] Document added.");

                } else if (command.startsWith("SEARCH")) {
                    String[] parts = command.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("[ERROR] Invalid SEARCH command format.");
                        continue;
                    }

                    String word = parts[1];
                    Set<String> results = invertedIndex.search(word);
                    if (results.isEmpty()) {
                        out.println("[SERVER] No files found containing the word: " + word);
                    } else {
                        out.println("[SERVER] Results: " + results);
                    }
                } else if (command.equals("EXIT")) {
                    out.println("[SERVER] Goodbye!");
                    break;
                } else {
                    out.println("[ERROR] Invalid command.");
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[SERVER] Error closing client socket: " + e.getMessage());
            }
        }
    }

    private class HttpSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("word=")) {
                String response = "Invalid query format. Use /search?word=<word>";
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            String word = query.substring(5);
            Set<String> results = invertedIndex.search(word);

            String response;
            if (results.isEmpty()) {
                response = "No files found containing the word: " + word;
            } else {
                response = "Results: " + results;
            }

            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
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