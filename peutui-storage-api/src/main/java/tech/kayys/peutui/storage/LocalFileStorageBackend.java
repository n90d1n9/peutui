package tech.kayys.peutui.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link StorageBackend} strategy that maps each key to a file under a root
 * directory, treating {@code "/"} in a key as a path separator. The
 * simplest, zero-dependency storage mode - suitable for a single-user CLI
 * or desktop app.
 */
public final class LocalFileStorageBackend implements StorageBackend {

    private final Path rootDir;

    public LocalFileStorageBackend(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create storage root: " + rootDir, e);
        }
    }

    @Override
    public void put(String key, byte[] value) {
        Path file = resolve(key);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, value);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write key: " + key, e);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        Path file = resolve(key);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete key: " + key, e);
        }
    }

    @Override
    public List<String> listKeys(String prefix) {
        List<String> keys = new ArrayList<>();
        Path searchRoot = prefix.isEmpty() ? rootDir : resolve(prefix).getParent();
        if (searchRoot == null || !Files.isDirectory(searchRoot)) {
            return keys;
        }
        try (Stream<Path> walk = Files.walk(searchRoot)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String key = rootDir.relativize(file).toString().replace('\\', '/');
                if (key.startsWith(prefix)) {
                    keys.add(key);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list keys with prefix: " + prefix, e);
        }
        return keys;
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public String describe() {
        return "local-file:" + rootDir;
    }

    private Path resolve(String key) {
        String sanitized = key.replace("..", "");
        return rootDir.resolve(sanitized);
    }
}
