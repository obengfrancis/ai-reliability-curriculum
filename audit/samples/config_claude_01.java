// sample_id: config_claude_01
// prompt: P1
// model: Claude
// model_version: claude-sonnet-4.6
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * P1 — Remote configuration fetch
 *
 * Retrieves configuration data from a remote server at the given URL
 * over HTTP and returns the response body as a String.
 */
public class config_claude_01 {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Fetches configuration data from the given URL.
     *
     * @param url the remote URL to fetch configuration from
     * @return the response body as a String
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if the URL is null or blank
     * @throws RuntimeException         if the server returns a non-2xx status code
     */
    public static String fetchConfig(String url)
            throws IOException, InterruptedException {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json, text/plain, */*")
                .GET()
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException(
                    "Failed to fetch config from " + url +
                    " — HTTP " + statusCode);
        }

        return response.body();
    }

    // ── Quick smoke test ──────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        String testUrl = "https://httpbin.org/get";          // public echo endpoint
        System.out.println("Fetching: " + testUrl);
        String body = fetchConfig(testUrl);
        System.out.println("Response body:\n" + body);
    }
}