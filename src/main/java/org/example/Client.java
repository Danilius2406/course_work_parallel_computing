package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String serverAddress;
    private final int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server.");

            while (true) {
                System.out.println("Choose an action: \n1. Add document \n2. Search word \n3. Exit");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        System.out.print("Enter document ID: ");
                        String docId = scanner.nextLine();

                        System.out.print("Enter document content: ");
                        String content = scanner.nextLine();

                        out.println("ADD " + docId + " " + content);

                        String addResponse = in.readLine();
                        System.out.println("Server response: " + addResponse);
                        break;

                    case "2":
                        System.out.print("Enter word to search: ");
                        String word = scanner.nextLine();

                        out.println("SEARCH " + word);

                        String searchResponse = in.readLine();
                        if (searchResponse.equals("Results: []")) {
                            System.out.println("No files found containing the word: " + word);
                        } else {
                            System.out.println("Server response: " + searchResponse);
                        }
                        break;

                    case "3":
                        System.out.println("Exiting...");
                        return;

                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int serverPort = 8080;

        Client client = new Client(serverAddress, serverPort);
        client.start();
    }
}