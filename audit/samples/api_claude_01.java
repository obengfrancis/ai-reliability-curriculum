// sample_id: api_claude_01
// prompt: P2
// model: Claude
// model_version: claude-sonnet-4-6
// date: 2026-05-29
// compiles: yes


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * P2 — REST API call
 *
 * Calls a REST API at https://api.example.com/users/{userId} and returns
 * the JSON response body as a String.
 */
public class api_claude_01 {

    private static final String BASE_URL = "https://api.example.com/users/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Retrieves the profile for the given user from the REST API.
     *
     * @param userId the ID of the user whose profile is to be fetched
     * @return the JSON response body as a String
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if userId is null or blank
     * @throws RuntimeException         if the server returns a non-2xx status code
     */
    public static String getUserProfile(String userId)
            throws IOException, InterruptedException {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }

        String url = BASE_URL + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException(
                    "Failed to fetch profile for userId=" + userId +
                    " — HTTP " + statusCode);
        }

        return response.body();
    }

    // ── Quick smoke test ──────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        String userId = "42";
        System.out.println("Fetching profile for userId: " + userId);
        String profile = getUserProfile(userId);
        System.out.println("Response body:\n" + profile);
    }
}