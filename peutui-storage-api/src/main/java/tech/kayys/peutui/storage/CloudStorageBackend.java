package tech.kayys.peutui.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link StorageBackend} strategy for a generic REST-style object store:
 * {@code PUT/GET/DELETE <baseUrl>/objects/<key>} for individual objects and
 * {@code GET <baseUrl>/objects?prefix=<prefix>} for listing, returning a
 * newline-separated key list. Deliberately protocol-minimal (no AWS SDK
 * dependency) so it works against S3-compatible gateways, a custom Quarkus
 * REST backend, or any similarly-shaped service behind an API gateway; swap
 * in a different {@code CloudStorageBackend}-shaped class for a bespoke
 * cloud API without touching session/settings code.
 */
public final class CloudStorageBackend implements StorageBackend {

    private final HttpClient client;
    private final URI baseUrl;
    private final Supplier<String> authorizationHeader;
    private final Duration timeout;

    public CloudStorageBackend(URI baseUrl, Supplier<String> authorizationHeader) {
        this(baseUrl, authorizationHeader, Duration.ofSeconds(30));
    }

    public CloudStorageBackend(URI baseUrl, Supplier<String> authorizationHeader, Duration timeout) {
        this.baseUrl = baseUrl;
        this.authorizationHeader = authorizationHeader;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public void put(String key, byte[] value) {
        HttpRequest request = requestBuilder("/objects/" + encode(key))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(value))
                .build();
        send(request, "put " + key);
    }

    @Override
    public Optional<byte[]> get(String key) {
        HttpRequest request = requestBuilder("/objects/" + encode(key)).GET().build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            requireSuccess(response.statusCode(), "get " + key);
            return Optional.of(response.body());
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new StorageException("Unable to get key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        HttpRequest request = requestBuilder("/objects/" + encode(key)).DELETE().build();
        send(request, "delete " + key);
    }

    @Override
    public List<String> listKeys(String prefix) {
        HttpRequest request = requestBuilder("/objects?prefix=" + encode(prefix)).GET().build();
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            requireSuccess(response.statusCode(), "list " + prefix);
            List<String> keys = new ArrayList<>();
            for (String line : response.body().split("\n")) {
                if (!line.isBlank()) {
                    keys.add(line.trim());
                }
            }
            return keys;
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new StorageException("Unable to list keys with prefix: " + prefix, e);
        }
    }

    @Override
    public boolean exists(String key) {
        HttpRequest request = requestBuilder("/objects/" + encode(key))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new StorageException("Unable to check existence of key: " + key, e);
        }
    }

    @Override
    public String describe() {
        return "cloud:" + baseUrl;
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUrl.resolve(path)).timeout(timeout);
        String auth = authorizationHeader.get();
        if (auth != null && !auth.isBlank()) {
            builder.header("Authorization", auth);
        }
        return builder;
    }

    private void send(HttpRequest request, String opDescription) {
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            requireSuccess(response.statusCode(), opDescription);
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new StorageException("Cloud storage operation failed: " + opDescription, e);
        }
    }

    private void requireSuccess(int statusCode, String opDescription) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new StorageException("Cloud storage operation '" + opDescription + "' failed with HTTP " + statusCode,
                    null);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
