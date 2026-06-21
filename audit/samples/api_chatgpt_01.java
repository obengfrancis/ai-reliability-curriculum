// sample_id: api_chatgpt_01
// prompt: P2
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class api_chatgpt_01 {
    public String getUserProfile(String userId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
        String apiUrl = "https://api.example.com/users/" + encodedUserId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to fetch user profile. HTTP status: " + response.statusCode());
        }

        return response.body();
    }
}