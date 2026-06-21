// sample_id: config_chatgpt_01
// prompt: P1
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class config_chatgpt_01 {
    public String fetchConfig(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to fetch configuration. HTTP status: " + response.statusCode());
        }

        return response.body();
    }
}