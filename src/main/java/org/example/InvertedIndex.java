package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndex {
    private final ConcurrentHashMap<String, Set<String>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    public void addDocument(String documentId, String content) {
        String[] words = content.split("\\W+");
        for (String word : words) {
            word = word.toLowerCase();
            index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(documentId);
        }
    }

    public Set<String> search(String word) {
        return index.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }

    public void loadDocumentsFromDirectory(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        addDocument(path.getFileName().toString(), content);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void printIndex() {
        for (Map.Entry<String, Set<String>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}