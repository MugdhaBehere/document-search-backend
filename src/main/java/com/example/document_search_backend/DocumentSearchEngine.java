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
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import opennlp.tools.stemmer.PorterStemmer;
import redis.clients.jedis.Jedis;

public class DocumentSearchEngine {
    private static final String DB_PATH = "rocksdb_data";
    private RocksDB db;
    private Map<String, String> documents = new ConcurrentHashMap<>();
    private Trie trie = new Trie();
    private BloomFilterService bloomFilterService = new BloomFilterService(1000);
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private Jedis redisClient = new Jedis("redis://localhost:6379");

    private static final String KAFKA_TOPIC = "document_index";
    private KafkaProducer<String, String> kafkaProducer;
    private PorterStemmer stemmer = new PorterStemmer();
    private Word2Vec word2Vec;

    static {
        RocksDB.loadLibrary();
    }

    public DocumentSearchEngine() {
        try {
            Options options = new Options().setCreateIfMissing(true);
            db = RocksDB.open(options, DB_PATH);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);
    }

    public void indexDocument(String docId, String content) {
        executor.submit(() -> {
            try {
                db.put(docId.getBytes(), content.getBytes());
                documents.put(docId, content);
                for (String word : content.split("\\W+")) {
                    bloomFilterService.add(word);
                    trie.insert(word);
                }
                kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, docId, content));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public List<String> search(String query) {
        List<String> expandedQueries = expandQuery(query);
        List<String> results = new ArrayList<>();
        
        for (String q : expandedQueries) {
            if (!bloomFilterService.mightContain(q)) {
                continue;
            }
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
