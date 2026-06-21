// sample_id: config_copilot_02
// prompt: P1
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

public class config_copilot_02 {
    public static String fetchConfig(String url) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("HTTP Error: " + response.statusCode());
        }

        return response.body();
    }
}