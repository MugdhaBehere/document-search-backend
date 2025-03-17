package com.example.document_search_backend;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")



@SpringBootApplication
@RestController
@RequestMapping("/search")
public class DocumentSearchAPI {
    private final DocumentSearchEngine engine = new DocumentSearchEngine();

    @PostMapping("/index")
    public String indexDocument(@RequestParam String docId, @RequestParam String content) {
        engine.indexDocument(docId, content);
        return "Document indexed successfully.";
    }

    @GetMapping("/query")
    public List<String> search(@RequestParam String query) {
        return engine.search(query);
    }

    @GetMapping("/suggestions")
    public List<String> getSuggestions(@RequestParam String prefix) {
        return engine.getSuggestions(prefix);
    }

    public static void main(String[] args) {
        SpringApplication.run(DocumentSearchAPI.class, args);
    }
}
