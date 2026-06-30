// sample_id: api_chatgpt_02
// prompt: P2
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-06-02
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class api_chatgpt_02 {

    public static String getUserProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }

        String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
        String url = "https://api.example.com/users/" + encodedUserId;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw new RuntimeException(
                    "Failed to fetch user profile. HTTP status: " + response.statusCode()
            );

        } catch (IOException e) {
            throw new RuntimeException("Network error while fetching user profile", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request was interrupted while fetching user profile", e);
        }
    }
}