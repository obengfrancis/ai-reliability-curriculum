// sample_id: config_claude_02
// prompt: P1
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

public class config_claude_02 {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches configuration data from the given URL and returns the response body.
     *
     * @param url the remote URL to fetch configuration from
     * @return the response body as a String
     * @throws IllegalArgumentException if the URL is null or blank
     * @throws RuntimeException         if the request fails or returns a non-2xx status
     */
    public static String fetchConfig(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json, text/plain, */*")
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Unexpected HTTP status " + statusCode + " fetching config from: " + url);
            }

            return response.body();

        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException("Request timed out fetching config from: " + url, e);
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to fetch config from: " + url, e);
        }
    }
}