// sample_id: config_claude_03
// prompt: P1
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class config_claude_03 {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches configuration data from a remote server at the given URL.
     *
     * @param url the HTTP endpoint to fetch config from
     * @return the response body as a String
     * @throws IllegalArgumentException if the URL is null or blank
     * @throws RuntimeException wrapping any I/O or HTTP error
     */
    public static String fetchConfig(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new RuntimeException(
                        "Config fetch failed — HTTP " + status + " from: " + url);
            }

            return response.body();

        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException("Request timed out fetching config from: " + url, e);
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // restore interrupt status
            }
            throw new RuntimeException("Failed to fetch config from: " + url, e);
        }
    }
}