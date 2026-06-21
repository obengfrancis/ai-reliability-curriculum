package edu.example.profileservice.storage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * Disk-backed cache store for serialized profile data. Profiles are
 * persisted as JSON files in {@code cacheDir}, one file per user id.
 */
public class CacheStore {

    private final Path cacheDir;

    public CacheStore(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * Write the serialized profile payload to disk. The cache file is
     * named {@code <userId>.json} within the configured cache directory.
     */
    public void write(String userId, String serializedPayload) throws IOException {
        Path cacheFile = cacheDir.resolve(userId + ".json");
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(cacheFile.toFile(), StandardCharsets.UTF_8));
        writer.write(serializedPayload);
        writer.flush();
        writer.close();
    }

    /**
     * Read the raw serialized payload for a user. Returns {@code null}
     * if the cache has no entry for this user.
     */
    public String readRaw(String userId) throws IOException {
        Path cacheFile = cacheDir.resolve(userId + ".json");
        if (!Files.exists(cacheFile)) {
            return null;
        }
        return Files.readString(cacheFile, StandardCharsets.UTF_8);
    }

    public boolean exists(String userId) {
        return Files.exists(cacheDir.resolve(userId + ".json"));
    }
}
