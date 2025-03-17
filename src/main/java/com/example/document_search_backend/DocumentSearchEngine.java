package com.example.document_search_backend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.rocksdb.RocksDBException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.document_search_backend.service.RocksDBService;

import opennlp.tools.stemmer.PorterStemmer;
import redis.clients.jedis.Jedis;
@RestController
public class DocumentSearchEngine {
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    private static final String KAFKA_TOPIC = "document_index";
    private static final String KAFKA_SERVER = System.getenv().getOrDefault("KAFKA_SERVER", "localhost:9092");

    private final RocksDBService rocksDBService;
    private final Map<String, String> documents = new ConcurrentHashMap<>();
    private final Trie trie = new Trie();
    private final BloomFilterService bloomFilterService = new BloomFilterService(1000);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Jedis redisClient = new Jedis(System.getenv().getOrDefault("REDIS_URL", DEFAULT_REDIS_URL));
    private final KafkaProducer<String, String> kafkaProducer;
    private final PorterStemmer stemmer = new PorterStemmer();

    public DocumentSearchEngine() throws RocksDBException {
        this.rocksDBService = new RocksDBService(); // Using RocksDB Service

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        kafkaProducer = new KafkaProducer<>(producerProps);
    }
        
    @PostMapping("/search/index")
    public void indexDocument(String docId, String content) {
        executor.submit(() -> {
            try {
                rocksDBService.put(docId, content); // Store in RocksDB
                documents.put(docId, content);

                for (String word : content.split("\\W+")) {
                    bloomFilterService.add(word);
                    trie.insert(word);
                }

                kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, docId, content));
                System.out.println("Document Indexed: " + docId);
            } catch (Exception e) {
                System.err.println("Error indexing document: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public List<String> search(String query) {
        List<String> expandedQueries = expandQuery(query);
        List<String> results = new ArrayList<>();

        for (String q : expandedQueries) {
            if (!bloomFilterService.mightContain(q)) continue;

            for (Map.Entry<String, String> entry : documents.entrySet()) {
                if (entry.getValue().contains(q)) {
                    results.add(entry.getKey());
                }
            }
        }
        return rankResults(query, results);
    }

    private List<String> expandQuery(String query) {
        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(query);

        for (String word : query.split(" ")) {
            expandedQueries.add(stemmer.stem(word));
        }
        return expandedQueries;
    }

    private List<String> rankResults(String query, List<String> results) {
        JaccardSimilarity similarity = new JaccardSimilarity();
        return results.stream()
                .sorted(Comparator.comparingDouble(doc -> -similarity.apply(query, documents.get(doc))))
                .collect(Collectors.toList());
    }

    public List<String> getSuggestions(String prefix) {
        return trie.getWordsWithPrefix(prefix);
    }
}
