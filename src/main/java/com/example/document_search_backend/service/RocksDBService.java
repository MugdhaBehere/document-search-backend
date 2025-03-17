package com.example.document_search_backend.service;

import java.io.File;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBService {
    private static final String DB_PATH = System.getenv().getOrDefault("ROCKSDB_PATH", "rocksdb_data");
    private final RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    public RocksDBService() throws RocksDBException {
        try {
            // ✅ Ensure directory exists before opening RocksDB
            File dbDir = new File(DB_PATH);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            Options options = new Options().setCreateIfMissing(true);
            this.db = RocksDB.open(options, DB_PATH);
        } catch (RocksDBException e) {
            System.err.println("Error initializing RocksDB: " + e.getMessage());
            throw e;
        }
    }

    public void put(String key, String value) throws RocksDBException {
        db.put(key.getBytes(), value.getBytes());
    }

    public String get(String key) throws RocksDBException {
        byte[] value = db.get(key.getBytes());
        return value != null ? new String(value) : null;
    }
}
