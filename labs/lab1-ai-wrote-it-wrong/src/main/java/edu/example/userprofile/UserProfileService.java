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
 * <p>Used by the lab module of the reliability curriculum. The intent is
 * that this class <em>looks</em> like the kind of code a popular AI coding
 * assistant might produce in response to a "build me a user profile client"
 * prompt: it compiles, runs cleanly on the happy path, and reads as
 * reasonable on first inspection. Closer reading reveals reliability
 * concerns that are the subject of the fault-analysis worksheet.
 */
public class UserProfileService {

    private final String baseUrl;
    private final Path cacheDir;
    private final ObjectMapper json = new ObjectMapper();

    public UserProfileService(String baseUrl, Path cacheDir) {
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
    }

    /**
     * Fetch a user profile by ID from the remote service.
     *
     * @param userId the user identifier
     * @return parsed profile as a map of field -> value
     * @throws IOException if the network call fails
     */
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

    /**
     * Update a user profile on the remote service. Sends the supplied
     * fields as a JSON body via HTTP POST.
     *
     * @param userId the user identifier
     * @param updates the fields to update
     * @return true on a 2xx response
     * @throws IOException if the network call fails
     */
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

    /**
     * Read a previously-cached profile from local disk. Returns null
     * when the cache has no entry for this user (e.g., first access).
     */
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

    /**
     * Fetch profiles for a batch of users. Profiles that cannot be
     * retrieved are omitted from the returned map.
     *
     * @param userIds the users whose profiles should be loaded
     * @return map of userId -> profile for users that loaded successfully
     */
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
