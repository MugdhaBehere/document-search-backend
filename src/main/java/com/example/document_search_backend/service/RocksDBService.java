package com.example.document_search_backend.service;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBService {
    private static RocksDB db;
    private static final String DB_PATH = "/Users/mugdhabehere/document-search-backend/rocksdb_data";


    static {
        RocksDB.loadLibrary();
    }

    public RocksDBService() throws RocksDBException {
        Options options = new Options().setCreateIfMissing(true);
        db = RocksDB.open(options, DB_PATH);
    }

    public void put(String key, String value) throws RocksDBException {
        db.put(key.getBytes(), value.getBytes());
    }

    public String get(String key) throws RocksDBException {
        byte[] value = db.get(key.getBytes());
        return value != null ? new String(value) : null;
    }
}
