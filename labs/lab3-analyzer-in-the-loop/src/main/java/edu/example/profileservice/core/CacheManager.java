package edu.example.profileservice.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.example.profileservice.storage.CacheStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for profile data, backed by a disk store for
 * persistence across restarts. Entries are loaded from disk on first
 * read and held in memory for subsequent lookups.
 */
public class CacheManager {

    private final CacheStore store;
    private final ObjectMapper json = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> PROFILE_TYPE =
        new TypeReference<>() {};
    private final Map<String, Map<String, Object>> memory = new ConcurrentHashMap<>();

    public CacheManager(CacheStore store) {
        this.store = store;
    }

    /**
     * Get the cached profile for a user. Checks the in-memory cache
     * and disk store; returns {@code null} if no cache entry exists.
     */
    public Map<String, Object> get(String userId) {
        try {
            String raw = store.readRaw(userId);
            if (raw == null) {
                return memory.get(userId);
            }
            Map<String, Object> deserialized = json.readValue(raw, PROFILE_TYPE);
           // Map<String, Object> deserialized = json.readValue(raw, HashMap.class);
            memory.put(userId, deserialized);
            return deserialized;
        } catch (IOException e) {
            return memory.get(userId);
        }
    }

    /**
     * Put a profile into both the in-memory cache and the disk store.
     */
    public void put(String userId, Map<String, Object> profile) throws IOException {
        memory.put(userId, profile);
        String serialized = json.writeValueAsString(profile);
        store.write(userId, serialized);
    }

    /**
     * Number of entries currently held in the in-memory cache.
     */
    public int size() {
        return memory.size();
    }
}
