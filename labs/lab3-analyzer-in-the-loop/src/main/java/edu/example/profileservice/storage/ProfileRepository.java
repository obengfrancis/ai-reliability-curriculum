package edu.example.profileservice.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence layer for user profiles. Backed by a remote HTTP store
 * in production; the demo runner points it at a local in-process
 * server.
 */
public class ProfileRepository {

    private final String baseUrl;
    private final ObjectMapper json = new ObjectMapper();

    public ProfileRepository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch a profile by id from the backing store.
     *
     * @throws ProfileNotFoundException if no profile exists for the id
     *         or if the store is unreachable
     */
    public Map<String, Object> findById(String userId) {
        URL url;
        HttpURLConnection conn;
        try {
            url = URI.create(baseUrl + "/profiles/" + userId).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
        } catch (IOException e) {
            throw new ProfileNotFoundException("could not reach store for " + userId);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return parseProfile(body.toString());
        } catch (IOException e) {
            throw new ProfileNotFoundException("failed to load profile " + userId);
        }
    }

    private Map<String, Object> parseProfile(String body) throws IOException {
        JsonNode root = json.readTree(body);
        Map<String, Object> out = new HashMap<>();
        root.fields().forEachRemaining(e ->
                out.put(e.getKey(), e.getValue().asText()));
        return out;
    }

    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }
}
