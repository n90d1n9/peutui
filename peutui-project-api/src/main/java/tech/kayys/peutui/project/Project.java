package tech.kayys.peutui.project;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * A workspace the TUI is operating against: a repo checkout, a data directory,
 * a cloud workspace id, etc.
 */
public record Project(String id, String name, Path rootPath, Instant createdAt, Map<String, String> metadata) {

    public static Project of(String id, String name, Path rootPath) {
        return new Project(id, name, rootPath, Instant.now(), Map.of());
    }
}
