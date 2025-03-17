package com.example.document_search_backend;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "doc-search-app.vercel.app")  // Allow frontend access
@RestController

@RequestMapping("/search")
public class DocumentSearchAPI {
    private final DocumentSearchEngine engine;

    public DocumentSearchAPI() {
        DocumentSearchEngine tempEngine;
        try {
            tempEngine = new DocumentSearchEngine();
        } catch (Exception e) {
            System.err.println("Error initializing DocumentSearchEngine: " + e.getMessage());
            e.printStackTrace();
            tempEngine = null; // Prevents application from crashing
        }
        this.engine = tempEngine;
    }

    @PostMapping("/index")
    public String indexDocument(@RequestParam String docId, @RequestParam String content) {
        if (engine == null) {
            return "Error: DocumentSearchEngine failed to initialize.";
        }
        engine.indexDocument(docId, content);
        return "Document indexed successfully.";
    }

    @GetMapping("/query")
    public List<String> search(@RequestParam String query) {
        if (engine == null) {
            return List.of("Error: Search engine failed to initialize.");
        }
        return engine.search(query);
    }

    @GetMapping("/suggestions")
    public List<String> getSuggestions(@RequestParam String prefix) {
        if (engine == null) {
            return List.of("Error: Suggestion engine failed to initialize.");
        }
        return engine.getSuggestions(prefix);
    }

    public static void main(String[] args) {
        SpringApplication.run(DocumentSearchAPI.class, args);
    }
}
