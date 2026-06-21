package edu.example.profileservice.api;

import edu.example.profileservice.core.ProfileService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Batch entry point for loading multiple profiles in parallel.
 * Submitted profiles are loaded concurrently against the underlying
 * profile service.
 */
public class BatchApi {

    private final ProfileService service;
    private final ExecutorService pool;

    public BatchApi(ProfileService service) {
        this.service = service;
        this.pool = Executors.newFixedThreadPool(8);
    }

    /**
     * Load all profiles in {@code userIds} in parallel. Returns a map
     * of {@code userId -> profile} for every user whose profile loaded
     * successfully.
     */
    public Map<String, Map<String, Object>> fetchAll(List<String> userIds) {
        Map<String, Future<Map<String, Object>>> futures = new HashMap<>();
        for (String userId : userIds) {
            futures.put(userId, pool.submit(() -> service.loadOrFetch(userId)));
        }

        Map<String, Map<String, Object>> results = new HashMap<>();
        for (Map.Entry<String, Future<Map<String, Object>>> e : futures.entrySet()) {
            try {
                Map<String, Object> profile = e.getValue().get();
                if (profile != null) {
                    results.put(e.getKey(), profile);
                }
            } catch (InterruptedException | ExecutionException ex) {
                System.err.println("Batch load failed for " + e.getKey()
                        + ": " + ex.getMessage());
            }
        }
        return results;
    }

    public void shutdown() {
        pool.shutdown();
    }
}
