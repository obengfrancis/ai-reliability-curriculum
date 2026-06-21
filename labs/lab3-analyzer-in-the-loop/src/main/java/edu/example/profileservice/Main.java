package edu.example.profileservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.example.profileservice.api.BatchApi;
import edu.example.profileservice.api.ProfileApi;
import edu.example.profileservice.core.CacheManager;
import edu.example.profileservice.core.NotificationDispatcher;
import edu.example.profileservice.core.ProfileService;
import edu.example.profileservice.external.AuditLogger;
import edu.example.profileservice.external.EmailGateway;
import edu.example.profileservice.storage.CacheStore;
import edu.example.profileservice.storage.ProfileRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Demo runner for the profile service codebase.
 *
 * <p>Stands up an in-process HTTP server that pretends to be the
 * remote profile API, the email gateway, and the audit log; wires
 * the service layers together; and exercises each public API once
 * against the in-process server.
 *
 * <p>Run with:
 * <pre>
 *   mvn compile exec:java
 * </pre>
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/profiles/", Main::handleProfile);
        server.createContext("/email", Main::handleEmail);
        server.createContext("/audit", Main::handleAudit);
        server.setExecutor(null);
        server.start();
        int port = server.getAddress().getPort();
        String baseUrl = "http://localhost:" + port;
        System.out.println("In-process API running at " + baseUrl);

        Path cacheDir = Files.createTempDirectory("profileservice-cache-");
        CacheStore cacheStore = new CacheStore(cacheDir);
        CacheManager cache = new CacheManager(cacheStore);
        ProfileRepository repo = new ProfileRepository(baseUrl);
        EmailGateway email = new EmailGateway(baseUrl);
        AuditLogger audit = new AuditLogger(baseUrl);
        NotificationDispatcher notifications = new NotificationDispatcher(email, audit);
        ProfileService service = new ProfileService(cache, repo, notifications);
        ProfileApi profileApi = new ProfileApi(service);
        BatchApi batchApi = new BatchApi(service);

        System.out.println("\n-- ProfileApi.getProfile --");
        Map<String, Object> profile = profileApi.getProfile("u-101");
        System.out.println("Fetched: " + profile);

        System.out.println("\n-- ProfileApi.getProfile (cache hit) --");
        Map<String, Object> cached = profileApi.getProfile("u-101");
        System.out.println("Re-fetched: " + cached);

        System.out.println("\n-- BatchApi.fetchAll --");
        Map<String, Map<String, Object>> batch = batchApi.fetchAll(
                List.of("u-201", "u-202", "u-203"));
        batch.forEach((id, p) -> System.out.println("  " + id + " -> " + p));

        batchApi.shutdown();
        server.stop(0);
        System.out.println("\nDemo complete. Cache directory was: " + cacheDir);
    }

    private static void handleProfile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        String body = "{\"id\":\"" + userId + "\","
                + "\"name\":\"User " + userId + "\","
                + "\"email\":\"" + userId + "@example.com\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void handleEmail(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(202, -1);
        ex.close();
    }

    private static void handleAudit(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(201, -1);
        ex.close();
    }
}
