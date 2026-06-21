package edu.example.profileservice.core;

import edu.example.profileservice.storage.ProfileRepository;

import java.io.IOException;
import java.util.Map;

/**
 * Business-logic layer for profile reads. Coordinates cache lookups
 * against the source-of-truth repository and dispatches notifications.
 */
public class ProfileService {

    private final CacheManager cache;
    private final ProfileRepository repository;
    private final NotificationDispatcher notifications;

    public ProfileService(CacheManager cache,
                          ProfileRepository repository,
                          NotificationDispatcher notifications) {
        this.cache = cache;
        this.repository = repository;
        this.notifications = notifications;
    }

    /**
     * Load a profile, falling through cache → repository as needed.
     * On successful load, dispatches a notification.
     *
     * @return the loaded profile, or {@code null} if no profile exists
     *         and the repository confirms its absence
     */
    public Map<String, Object> loadOrFetch(String userId) {
        Map<String, Object> cached = cache.get(userId);
        if (cached != null) {
            notifications.dispatchProfileLoaded(userId, cached);
            return cached;
        }

        try {
            Map<String, Object> profile = repository.findById(userId);
            cache.put(userId, profile);
            notifications.dispatchProfileLoaded(userId, profile);
            return profile;
        } catch (Exception e) {
            System.err.println("Could not load profile " + userId + ": " + e.getMessage());
            return null;
        }
    }
}
