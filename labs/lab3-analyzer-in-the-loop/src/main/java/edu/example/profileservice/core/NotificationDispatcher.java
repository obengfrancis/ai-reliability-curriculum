package edu.example.profileservice.core;

import edu.example.profileservice.external.AuditLogger;
import edu.example.profileservice.external.EmailGateway;

import java.util.Map;

/**
 * Coordinates outbound notifications for profile lifecycle events:
 * sends transactional email via {@link EmailGateway} and records the
 * event in the audit log via {@link AuditLogger}.
 */
public class NotificationDispatcher {

    private final EmailGateway email;
    private final AuditLogger audit;

    public NotificationDispatcher(EmailGateway email, AuditLogger audit) {
        this.email = email;
        this.audit = audit;
    }

    /**
     * Dispatch a notification for a profile event. Sends the email
     * first; on success, appends an audit entry.
     */
    public void dispatchProfileLoaded(String userId, Map<String, Object> profile) {
        Object emailAddress = profile.get("email");
        if (emailAddress == null) {
            return;
        }
        boolean sent = email.send(
                String.valueOf(emailAddress),
                "Your profile was accessed",
                "Hello, your profile (id=" + userId + ") was accessed at "
                        + java.time.Instant.now());
        if (sent) {
            audit.append("system", "profile_loaded_notification", userId);
        }
    }
}
