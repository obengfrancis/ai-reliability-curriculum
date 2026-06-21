package edu.example.userprofile;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Demo runner for the UserProfileService.
 *
 * <p>Spins up a small in-process HTTP server that pretends to be the
 * remote user-profile API, exercises each public method of the service
 * once, and prints the results. This lets students see the code work
 * end-to-end on the happy path before they begin the fault-analysis
 * worksheet for Lab 1.
 *
 * <p>To run:
 * <pre>
 *   mvn compile exec:java
 * </pre>
 */
public final class Main {

    private Main() {} // utility

    public static void main(String[] args) throws Exception {
        // 1. Stand up a tiny in-process HTTP server.
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/users/", Main::handleUserRequest);
        server.setExecutor(null); // single-threaded; fine for a demo
        server.start();
        int port = server.getAddress().getPort();
        String baseUrl = "http://localhost:" + port;
        System.out.println("Fake profile API listening at " + baseUrl);

        // 2. Set up a temp cache directory.
        Path cacheDir = Files.createTempDirectory("profile-cache-");
        UserProfileService service = new UserProfileService(baseUrl, cacheDir);

        // 3. Exercise each method once.

        System.out.println("\n-- fetchUserProfile --");
        Map<String, Object> profile = service.fetchUserProfile("u-101");
        System.out.println("Fetched: " + profile);

        // Seed the cache so the cached-read demo has something to read.
        Files.writeString(
                cacheDir.resolve("u-101.json"),
                "{\"id\":\"u-101\",\"name\":\"Cached Name\",\"email\":\"cached@example.com\"}",
                StandardCharsets.UTF_8);

        System.out.println("\n-- getCachedProfile --");
        Map<String, Object> cached = service.getCachedProfile("u-101");
        System.out.println("Cached: " + cached);

        System.out.println("\n-- updateProfile --");
        boolean ok = service.updateProfile("u-101", Map.of("email", "new@example.com"));
        System.out.println("Update succeeded: " + ok);

        System.out.println("\n-- loadProfileBatch --");
        Map<String, Map<String, Object>> batch =
                service.loadProfileBatch(List.of("u-101", "u-102", "u-103"));
        batch.forEach((id, p) -> System.out.println("  " + id + " -> " + p));

        // 4. Clean up.
        server.stop(0);
        System.out.println("\nDemo complete.");
    }

    /**
     * Canned handler for /users/{id}:
     *   GET  -> 200 with a small JSON profile
     *   POST -> 200 with empty body
     */
    private static void handleUserRequest(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            String body = "{\"id\":\"" + userId + "\","
                    + "\"name\":\"User " + userId + "\","
                    + "\"email\":\"" + userId + "@example.com\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(200, -1);
        } else {
            ex.sendResponseHeaders(405, -1);
        }
        ex.close();
    }
}
