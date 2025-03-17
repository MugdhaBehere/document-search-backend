package com.example.document_search_backend;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;

public class BloomFilterService {
    private BloomFilter<String> bloomFilter;

    public BloomFilterService(int expectedInsertions) {
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions);
    }

    public void add(String word) {
        bloomFilter.put(word);
    }

    public boolean mightContain(String word) {
        return bloomFilter.mightContain(word);
    }
}
