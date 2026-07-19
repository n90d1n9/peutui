package tech.kayys.peutui.settings;

import java.util.Map;
import java.util.Optional;

/**
 * Persistence strategy for a single {@link SettingsScope}. Implementations
 * decide *where* that scope's key/value pairs live (local file, local
 * database, cloud). A given application typically wires one
 * {@code SettingsStore} per scope, possibly all backed by the same
 * underlying {@code StorageBackend} with different key prefixes.
 */
public interface SettingsStore {

    Optional<String> get(String scopeId, String key);

    Map<String, String> getAll(String scopeId);

    void set(String scopeId, String key, String value);

    void remove(String scopeId, String key);
}
