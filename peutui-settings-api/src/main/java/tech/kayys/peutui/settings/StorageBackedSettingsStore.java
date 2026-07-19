package tech.kayys.peutui.settings;

import tech.kayys.peutui.storage.StorageBackend;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link SettingsStore} strategy that persists one JSON object per scope
 * instance through any {@link StorageBackend} - local file, local database,
 * or cloud. Mirrors {@link StorageBackedSessionStore}'s approach so both
 * sessions and settings can share a single configured storage mode.
 */
public final class StorageBackedSettingsStore implements SettingsStore {

    private static final String KEY_PREFIX = "settings/";

    private final StorageBackend backend;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    public StorageBackedSettingsStore(StorageBackend backend) {
        this.backend = backend;
    }

    @Override
    public Optional<String> get(String scopeId, String key) {
        return Optional.ofNullable(readAll(scopeId).get(key));
    }

    @Override
    public Map<String, String> getAll(String scopeId) {
        return Map.copyOf(readAll(scopeId));
    }

    @Override
    public void set(String scopeId, String key, String value) {
        lock.lock();
        try {
            Map<String, String> all = readAll(scopeId);
            all.put(key, value);
            writeAll(scopeId, all);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String scopeId, String key) {
        lock.lock();
        try {
            Map<String, String> all = readAll(scopeId);
            all.remove(key);
            writeAll(scopeId, all);
        } finally {
            lock.unlock();
        }
    }

    private Map<String, String> readAll(String scopeId) {
        return backend.get(keyFor(scopeId)).map(bytes -> {
            try {
                return (Map<String, String>) mapper.readValue(
                        new String(bytes, StandardCharsets.UTF_8), new TypeReference<LinkedHashMap<String, String>>() {
                        });
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Unable to read settings scope " + scopeId, e);
            }
        }).orElseGet(LinkedHashMap::new);
    }

    private void writeAll(String scopeId, Map<String, String> values) {
        try {
            backend.put(keyFor(scopeId), mapper.writeValueAsBytes(values));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to write settings scope " + scopeId, e);
        }
    }

    private static String keyFor(String scopeId) {
        return KEY_PREFIX + scopeId + ".json";
    }
}
