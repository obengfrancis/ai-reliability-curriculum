// sample_id: api_copilot_03
// prompt: P2
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class api_copilot_03 {
    
    private static final HttpClient client = HttpClient.newHttpClient();
    
    public static String getUserProfile(String userId) throws Exception {
        String apiUrl = "https://api.example.com/users/" + userId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new java.net.URI(apiUrl))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get user profile. HTTP error code: " + response.statusCode());
        }
        
        return response.body();
    }
}