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
        httpServer.createContext("/add", new HttpAddHandler());
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

    private void addCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private class HttpSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("[HTTP] Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

            addCORSHeaders(exchange);

            if (!"GET".equals(exchange.getRequestMethod())) {
                System.out.println("[HTTP] Method not allowed: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("word=")) {
                String response = "Invalid query format. Use /search?word=<word>";
                System.out.println("[HTTP] Invalid query format.");
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
                System.out.println("[HTTP] No results for word: " + word);
            } else {
                response = "Results: " + results;
                System.out.println("[HTTP] Search results for word '" + word + "': " + results);
            }

            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        }
    }

    private class HttpAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCORSHeaders(exchange);

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream inputStream = exchange.getRequestBody();
            String body = new String(inputStream.readAllBytes());

            String[] pairs = body.split("&");
            String docId = null;
            String content = null;

            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");

                    if ("docId".equals(key)) {
                        docId = value;
                    } else if ("content".equals(key)) {
                        content = value;
                    }
                }
            }

            if (docId == null || content == null) {
                String response = "Missing docId or content in form data.";
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            try {
                String filePath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset\\" + docId + ".txt";
                Files.writeString(Paths.get(filePath), content);

                invertedIndex.addDocument(docId, content);

                System.out.println("[HTTP] Document added: " + docId);
                System.out.println("[HTTP] Content: " + content);

                String response = "Document added successfully.";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
            } catch (IOException e) {
                String response = "Error saving document: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.length());
                exchange.getResponseBody().write(response.getBytes());
            } finally {
                exchange.getResponseBody().close();
            }
        }
    }

    public void stop() {
        threadPool.shutdown();
        System.out.println("[SERVER] Server stopped.");
    }

    public static void main(String[] args) {
        String datasetPath = "C:\\Users\\Danie\\Desktop\\4 course 1 term\\ParallelComputing\\Dataset";
        Server server = new Server(8080, 16);
        server.start(datasetPath);
    }
}
