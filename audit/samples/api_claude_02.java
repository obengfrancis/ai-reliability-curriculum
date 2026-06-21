// sample_id: api_claude_02
// prompt: P2
// model: Claude
// model_version: claude-sonnet-4.6
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class api_claude_02 {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String getUserProfile(String userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/users/" + userId))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error: HTTP " + response.statusCode()
                    + " for userId=" + userId);
        }

        return response.body();
    }
}