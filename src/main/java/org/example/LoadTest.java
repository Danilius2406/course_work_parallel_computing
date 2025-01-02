package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTest {
    private static final int NUM_CLIENTS = 100;
    private static final int NUM_REQUESTS_PER_CLIENT = 30;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong successfulRequests = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            executor.submit(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    for (int j = 0; j < NUM_REQUESTS_PER_CLIENT; j++) {
                        long requestStartTime = System.nanoTime();

                        out.println("SEARCH test");

                        String response = in.readLine();
                        if (response != null) {
                            successfulRequests.incrementAndGet();
                        }

                        long requestEndTime = System.nanoTime();
                        totalResponseTime.addAndGet(requestEndTime - requestStartTime);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        double averageResponseTime = totalResponseTime.get() / (double) successfulRequests.get() / 1_000_000.0; // мс

        System.out.println("Тестування завершено.");
        System.out.println("Загальний час тестування: " + (totalTime / 1_000_000_000.0) + " секунд");
        System.out.println("Кількість успішних запитів: " + successfulRequests.get());
        System.out.println("Середній час відповіді: " + averageResponseTime + " мс");
    }
}