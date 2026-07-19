package tech.kayys.peutui.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link SettingsStore} strategy that persists each scope instance as one
 * JSON object file ({@code <root>/<scopeId>.json}) - a "local file" storage
 * mode with no external services, matching the same pattern used by
 * {@code FileSessionStore}.
 */
public final class FileSettingsStore implements SettingsStore {

    private final Path rootDir;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    public FileSettingsStore(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create settings store directory: " + rootDir, e);
        }
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
        Path file = fileFor(scopeId);
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            return mapper.readValue(file.toFile(), new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read settings scope " + scopeId, e);
        }
    }

    private void writeAll(String scopeId, Map<String, String> values) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fileFor(scopeId).toFile(), values);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write settings scope " + scopeId, e);
        }
    }

    private Path fileFor(String scopeId) {
        return rootDir.resolve(scopeId + ".json");
    }
}
