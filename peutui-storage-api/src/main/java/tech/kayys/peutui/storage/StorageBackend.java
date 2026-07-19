package tech.kayys.peutui.storage;

import java.util.List;
import java.util.Optional;

/**
 * Generic key/value + blob persistence strategy underlying sessions,
 * settings, and any other data the host application wants durable. Keys are
 * opaque strings; implementations are free to interpret a {@code "/"}
 * separated key as a hierarchy (directory path, DB namespace, object
 * prefix) as suits their backend.
 */
public interface StorageBackend {

    void put(String key, byte[] value);

    Optional<byte[]> get(String key);

    void delete(String key);

    /**
     * Lists all keys beginning with {@code prefix} (pass {@code ""} for
     * everything).
     */
    List<String> listKeys(String prefix);

    boolean exists(String key);

    /**
     * A short, human-readable identifier for logging/diagnostics (e.g.
     * "local-file", "sqlite", "s3").
     */
    String describe();
}
