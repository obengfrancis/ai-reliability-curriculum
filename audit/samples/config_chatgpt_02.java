// sample_id: config_chatgpt_02
// prompt: P1
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-06-02
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class config_chatgpt_02 {

    public static String fetchConfig(String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }

        throw new IOException("Failed to fetch configuration. HTTP status: "
                + response.statusCode());
    }
}