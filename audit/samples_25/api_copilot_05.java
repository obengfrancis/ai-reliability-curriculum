// sample_id: api_copilot_05
// prompt: P2
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class api_copilot_05 {
    private static final String API_BASE_URL = "https://api.example.com/users";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Retrieves the user profile from the REST API.
     *
     * @param userId the ID of the user to retrieve
     * @return the JSON response body as a String
     * @throws RuntimeException if the HTTP request fails or returns an error status
     */
    public static String getUserProfile(String userId) {
        String url = API_BASE_URL + "/" + userId;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                throw new RuntimeException("API Error: HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile: " + e.getMessage(), e);
        }
    }
}