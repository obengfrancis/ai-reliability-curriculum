// sample_id: api_claude_05
// prompt: P2
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class api_claude_05 {

    private static final String BASE_URL = "https://api.example.com/users/";
    private final HttpClient httpClient;

    public api_claude_05() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getUserProfile(String userId) throws IOException, InterruptedException {
        String encodedId = URLEncoder.encode(userId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + encodedId))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected response status: " + response.statusCode());
        }

        return response.body();
    }
}