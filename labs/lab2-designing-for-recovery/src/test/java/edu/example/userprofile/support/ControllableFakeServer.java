package edu.example.userprofile.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small in-process HTTP server with a fluent API for injecting
 * failures. Used by the Lab 2 test harness to give the student's
 * retry and circuit-breaker implementations realistic operations to
 * wrap.
 *
 * <p>The server exposes a single endpoint pattern {@code /users/<id>}.
 * What that endpoint does on each request is controlled by a queue
 * of <em>scripted responses</em> set up before the test. When the
 * script is exhausted, the server defaults to returning {@code 200}
 * with a stub profile body.
 *
 * <p>Example use:
 * <pre>{@code
 *   try (ControllableFakeServer server = ControllableFakeServer.start()) {
 *       server.failNext(2, 503).thenSucceed();
 *       // ... student code makes 3 calls; first 2 fail, third succeeds
 *   }
 * }</pre>
 *
 * <p>The server uses an OS-assigned port. Get the base URL via
 * {@link #baseUrl()}.
 */
public final class ControllableFakeServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;
    private final Deque<Response> script = new ArrayDeque<>();
    private final AtomicInteger requestCount = new AtomicInteger(0);

    private ControllableFakeServer(HttpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    public static ControllableFakeServer start() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        ControllableFakeServer cfs = new ControllableFakeServer(s, s.getAddress().getPort());
        s.createContext("/users/", cfs::handle);
        s.setExecutor(null);
        s.start();
        return cfs;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public int requestCount() {
        return requestCount.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ---- fluent API ---------------------------------------------------

    /**
     * Script the next {@code count} requests to fail with the given
     * HTTP status code. Subsequent requests fall through to whatever
     * the script holds after these (often {@link #thenSucceed()}).
     */
    public ControllableFakeServer failNext(int count, int statusCode) {
        for (int i = 0; i < count; i++) {
            script.addLast(new FailResponse(statusCode));
        }
        return this;
    }

    /**
     * Script the next request to delay {@code delayMs} milliseconds
     * before returning {@code 200} with a stub profile body. Use
     * this to test timeout configuration.
     */
    public ControllableFakeServer slowNext(int count, long delayMs) {
        for (int i = 0; i < count; i++) {
            script.addLast(new SlowResponse(delayMs));
        }
        return this;
    }

    /**
     * Script the next request to succeed with {@code 200} and a stub
     * profile body. Use this after {@code failNext(...)} to set up
     * recovery scenarios.
     */
    public ControllableFakeServer succeedNext(int count) {
        for (int i = 0; i < count; i++) {
            script.addLast(new SuccessResponse());
        }
        return this;
    }

    /**
     * After the currently-scripted failures, all subsequent requests
     * succeed. Functionally equivalent to clearing the script after
     * the failures are consumed; included for readability:
     * {@code server.failNext(2, 503).thenSucceed();}
     */
    public ControllableFakeServer thenSucceed() {
        // No-op: when the script is empty, the default behaviour is
        // already "succeed with stub profile". This method exists so
        // tests read naturally.
        return this;
    }

    // ---- handler ------------------------------------------------------

    private void handle(HttpExchange ex) throws IOException {
        int requestNum = requestCount.incrementAndGet();
        Response next;
        synchronized (script) {
            next = script.pollFirst();
        }
        if (next == null) {
            next = new SuccessResponse();
        }
        next.write(ex, requestNum);
    }

    // ---- response types ----------------------------------------------

    private interface Response {
        void write(HttpExchange ex, int requestNum) throws IOException;
    }

    private static final class SuccessResponse implements Response {
        @Override
        public void write(HttpExchange ex, int requestNum) throws IOException {
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
    }

    private static final class FailResponse implements Response {
        private final int statusCode;

        FailResponse(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public void write(HttpExchange ex, int requestNum) throws IOException {
            String body = "{\"error\":\"Injected failure\",\"status\":" + statusCode + "}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(statusCode, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        }
    }

    private static final class SlowResponse implements Response {
        private final long delayMs;

        SlowResponse(long delayMs) {
            this.delayMs = delayMs;
        }

        @Override
        public void write(HttpExchange ex, int requestNum) throws IOException {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Fall through and respond anyway.
            }
            new SuccessResponse().write(ex, requestNum);
        }
    }
}
