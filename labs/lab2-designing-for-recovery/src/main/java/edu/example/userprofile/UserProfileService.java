package edu.example.userprofile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client service that retrieves and manages user profile data from a
 * remote HTTP API, with a local on-disk cache for quick re-reads.
 *
 * <p>This is the same {@code UserProfileService} as in Lab 1 — same
 * four reliability defects intact. In Lab 2, students wrap calls to
 * this service with the retry and circuit-breaker patterns they
 * implement in the {@code resilience} package. The service itself
 * is unchanged.
 */
public class UserProfileService {

    private final String baseUrl;
    private final Path cacheDir;
    private final ObjectMapper json = new ObjectMapper();

    public UserProfileService(String baseUrl, Path cacheDir) {
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
    }

    public Map<String, Object> fetchUserProfile(String userId) throws IOException {
        URL url = URI.create(baseUrl + "/users/" + userId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return parseProfile(body.toString());
        }
    }

    public boolean updateProfile(String userId, Map<String, Object> updates) throws IOException {
        URL url = URI.create(baseUrl + "/users/" + userId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = json.writeValueAsString(updates);
        OutputStream out = conn.getOutputStream();
        out.write(payload.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

        int status = conn.getResponseCode();
        conn.disconnect();
        return status >= 200 && status < 300;
    }

    public Map<String, Object> getCachedProfile(String userId) {
        Path cacheFile = cacheDir.resolve(userId + ".json");
        try {
            String contents = Files.readString(cacheFile, StandardCharsets.UTF_8);
            return parseProfile(contents);
        } catch (Exception e) {
            System.err.println("Cache read failed for " + userId + ": " + e.getMessage());
            return null;
        }
    }

    public Map<String, Map<String, Object>> loadProfileBatch(List<String> userIds) {
        Map<String, Map<String, Object>> results = new HashMap<>();
        List<String> failed = new ArrayList<>();

        for (String userId : userIds) {
            try {
                Map<String, Object> profile = fetchUserProfile(userId);
                results.put(userId, profile);
            } catch (IOException e) {
                failed.add(userId);
            }
        }

        if (!failed.isEmpty()) {
            System.err.println("Could not load profiles for: " + failed);
        }
        return results;
    }

    // -- helpers ----------------------------------------------------------

    private Map<String, Object> parseProfile(String body) throws IOException {
        JsonNode root = json.readTree(body);
        Map<String, Object> out = new HashMap<>();
        root.fields().forEachRemaining(e ->
                out.put(e.getKey(), e.getValue().asText()));
        return out;
    }
}
