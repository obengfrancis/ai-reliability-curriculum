package edu.example.profileservice.external;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Outbound gateway for transactional emails sent in response to
 * profile lifecycle events (welcome, profile-updated, etc.).
 *
 * <p>Backed by an HTTP API at {@code baseUrl}; the demo runner points
 * it at a local in-process server.
 */
public class EmailGateway {

    private final String baseUrl;

    public EmailGateway(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Submit an outbound email to the gateway. Returns true if the
     * gateway accepted the message (any 2xx response).
     */
    public boolean send(String recipient, String subject, String body) {
        try {
            URL url = URI.create(baseUrl + "/email").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"to\":\"" + recipient + "\","
                    + "\"subject\":\"" + subject + "\","
                    + "\"body\":\"" + body + "\"}";

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            conn.disconnect();
            return status >= 200 && status < 300;
        } catch (IOException e) {
        }
        return false;
    }
}
