package com.example.document_search_backend;

import java.util.List;
import java.util.Scanner;

public class DocumentSearchCLI {
    private static final DocumentSearchEngine engine = new DocumentSearchEngine();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Document Search CLI!");
        
        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Index a document");
            System.out.println("2. Search for a document");
            System.out.println("3. Get word suggestions");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            switch (choice) {
                case 1:
                    System.out.print("Enter document ID: ");
                    String docId = scanner.nextLine();
                    System.out.print("Enter document content: ");
                    String content = scanner.nextLine();
                    engine.indexDocument(docId, content);
                    System.out.println("Document indexed successfully.");
                    break;
                
                case 2:
                    System.out.print("Enter search query: ");
                    String query = scanner.nextLine();
                    List<String> results = engine.search(query);
                    if (results.isEmpty()) {
                        System.out.println("No matching documents found.");
                    } else {
                        System.out.println("Matching document IDs: " + results);
                    }
                    break;
                
                case 3:
                    System.out.print("Enter prefix for suggestions: ");
                    String prefix = scanner.nextLine();
                    List<String> suggestions = engine.getSuggestions(prefix);
                    System.out.println("Suggestions: " + suggestions);
                    break;
                
                case 4:
                    System.out.println("Exiting Document Search CLI.");
                    scanner.close();
                    return;
                
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
