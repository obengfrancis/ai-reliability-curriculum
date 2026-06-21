// sample_id: config_claude_05
// prompt: P1
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class config_claude_05 {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    /**
     * Fetches remote configuration from the given URL.
     *
     * @param url the HTTP/HTTPS endpoint to retrieve configuration from
     * @return the response body as a String
     * @throws IOException          on I/O errors or non-2xx HTTP responses
     * @throws InterruptedException if the request is interrupted
     */
    public String fetchConfig(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException(
                "Config fetch failed — HTTP " + status + " from: " + url);
        }

        return response.body();
    }
}