// sample_id: config_claude_04
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

public class config_claude_04 {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches configuration data from a remote server.
     *
     * @param url the URL of the configuration endpoint
     * @return the response body as a String
     * @throws IllegalArgumentException if the URL is null or blank
     * @throws ConfigFetchException     if the request fails or returns a non-2xx status
     */
    public String fetchConfig(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Config URL must not be null or blank");
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

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new ConfigFetchException(
                        "Config fetch failed — HTTP " + status + " from: " + url);
            }

            return response.body();

        } catch (ConfigFetchException e) {
            throw e; // re-throw without wrapping
        } catch (IllegalArgumentException e) {
            throw new ConfigFetchException("Invalid URL: " + url, e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ConfigFetchException("Request timed out fetching config from: " + url, e);
        } catch (java.io.IOException e) {
            throw new ConfigFetchException("I/O error fetching config from: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            throw new ConfigFetchException("Interrupted while fetching config from: " + url, e);
        }
    }

    /** Typed exception for config-fetch failures. */
    public static class ConfigFetchException extends RuntimeException {
        public ConfigFetchException(String message) { super(message); }
        public ConfigFetchException(String message, Throwable cause) { super(message, cause); }
    }
}