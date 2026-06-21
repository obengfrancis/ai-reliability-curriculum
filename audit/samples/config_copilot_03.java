// sample_id: config_copilot_03
// prompt: P1
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class config_copilot_03 {
    
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    /**
     * Fetches configuration data from a remote server.
     * 
     * @param url the URL to fetch configuration from
     * @return the response body as a String
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static String fetchConfig(String url) throws IOException, InterruptedException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
}