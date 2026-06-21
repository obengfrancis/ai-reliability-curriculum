package edu.example.profileservice.api;

import edu.example.profileservice.core.ProfileService;

import java.util.Map;

/**
 * Top-level entry point for single-profile read operations.
 */
public class ProfileApi {

    private final ProfileService service;

    public ProfileApi(ProfileService service) {
        this.service = service;
    }

    /**
     * Fetch a single profile by id. Returns the profile or {@code null}
     * if no profile exists for the given id.
     */
    public Map<String, Object> getProfile(String userId) {
        return service.loadOrFetch(userId);
    }
}
