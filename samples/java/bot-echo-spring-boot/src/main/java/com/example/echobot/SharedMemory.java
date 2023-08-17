package com.example.echobot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class SharedMemory {
    private Map<String, String> cache = new HashMap<>();

    public SharedMemory() {
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }

    public String get(String key) {
        return cache.get(key);
    }
}
