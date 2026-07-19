package tech.kayys.peutui.settings;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SettingsStore} strategy holding values in a concurrent in-memory map,
 * keyed by scopeId then setting key.
 */
public final class InMemorySettingsStore implements SettingsStore {

    private final Map<String, Map<String, String>> data = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String scopeId, String key) {
        return Optional.ofNullable(data.getOrDefault(scopeId, Map.of()).get(key));
    }

    @Override
    public Map<String, String> getAll(String scopeId) {
        return Map.copyOf(data.getOrDefault(scopeId, Map.of()));
    }

    @Override
    public void set(String scopeId, String key, String value) {
        data.computeIfAbsent(scopeId, id -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Override
    public void remove(String scopeId, String key) {
        Map<String, String> scoped = data.get(scopeId);
        if (scoped != null) {
            scoped.remove(key);
        }
    }
}
