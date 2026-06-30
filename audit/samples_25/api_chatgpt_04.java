// sample_id: api_chatgpt_04
// prompt: P2
// model: ChatGPT
// model_version: GPT-5.5 Thinking
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

public class api_chatgpt_04 {

    public static String getUserProfile(String userId) throws IOException, InterruptedException {
        String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
        String url = "https://api.example.com/users/" + encodedUserId;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }

        throw new IOException("API request failed with status code: " + response.statusCode());
    }
}