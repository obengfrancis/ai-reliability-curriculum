// sample_id: api_claude_04
// prompt: P2
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class api_claude_04 {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String BASE_URL = "https://api.example.com/users/";

    public String getUserProfile(String userId) throws IOException, InterruptedException {
        URI uri = URI.create(BASE_URL + userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException(
                "Unexpected HTTP status: " + statusCode + " for userId: " + userId
            );
        }

        return response.body();
    }
}