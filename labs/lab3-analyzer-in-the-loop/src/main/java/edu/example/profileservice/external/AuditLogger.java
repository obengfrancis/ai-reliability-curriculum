package edu.example.profileservice.external;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Append-only audit log for compliance-relevant events (profile reads,
 * updates, deletions). Persists each event to a remote append-only
 * service.
 */
public class AuditLogger {

    private final String baseUrl;

    public AuditLogger(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Append an audit event to the remote log. Each event records the
     * actor, action, and target user id along with a server-side
     * timestamp.
     *
     * @return true if the audit event was accepted by the log service
     */
    public boolean append(String actor, String action, String targetUserId) {
        try {
            URL url = URI.create(baseUrl + "/audit").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setDoOutput(true);

            String payload = String.format(
                    "{\"actor\":\"%s\",\"action\":\"%s\",\"target\":\"%s\",\"ts\":\"%s\"}",
                    actor, action, targetUserId, Instant.now().toString());

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            conn.disconnect();
            return status >= 200 && status < 300;
        } catch (IOException e) {
            System.err.println("Audit append failed: " + e.getMessage());
            return false;
        }
    }
}
