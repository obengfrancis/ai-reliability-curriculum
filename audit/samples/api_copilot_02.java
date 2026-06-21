// sample_id: api_copilot_02
// prompt: P2
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class api_copilot_02 {
    
    public static String getUserProfile(String userId) throws Exception {
        // Create an HttpClient
        HttpClient client = HttpClient.newHttpClient();
        
        // Build the URL with the userId parameter
        String url = "https://api.example.com/users/" + userId;
        
        // Create an HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        
        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Check if the request was successful
        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status: " + response.statusCode());
        }
        
        // Return the response body as a String
        return response.body();
    }
}