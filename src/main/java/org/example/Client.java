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
        try (Socket socket = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server.");

            while (true) {
                System.out.println();
                System.out.println("Choose an action: ");
                System.out.println("1. Add document ");
                System.out.println("2. Search word ");
                System.out.println("3. Exit");
                System.out.print("Choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                if (choice == 1) {
                    System.out.print("Enter document ID: ");
                    String docId = scanner.nextLine();
                    System.out.print("Enter document content: ");
                    String content = scanner.nextLine();
                    out.println("ADD " + docId + " " + content);
                } else if (choice == 2) {
                    System.out.print("Enter word to search: ");
                    String word = scanner.nextLine();
                    out.println("SEARCH " + word);
                } else if (choice == 3) {
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid choice.");
                    continue;
                }

                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println("Server response: " + response);
                    break;
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