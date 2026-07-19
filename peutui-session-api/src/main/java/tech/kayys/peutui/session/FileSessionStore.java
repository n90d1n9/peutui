package tech.kayys.peutui.session;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link SessionStore} strategy that persists each session as one JSON file
 * under a root directory ({@code <root>/<sessionId>.json}), suitable for a
 * "local file" storage mode with zero external services.
 */
public final class FileSessionStore implements SessionStore {

    private final Path rootDir;
    private final ObjectMapper mapper;

    public FileSessionStore(Path rootDir) {
        this.rootDir = rootDir;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create session store directory: " + rootDir, e);
        }
    }

    @Override
    public Session save(Session session) {
        Path file = fileFor(session.id());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), SessionDto.from(session));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to save session " + session.id(), e);
        }
        return session;
    }

    @Override
    public Optional<Session> load(String sessionId) {
        Path file = fileFor(sessionId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            SessionDto dto = mapper.readValue(file.toFile(), SessionDto.class);
            return Optional.of(dto.toSession());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load session " + sessionId, e);
        }
    }

    @Override
    public List<Session> listByProject(String projectId) {
        List<Session> result = new ArrayList<>();
        if (!Files.isDirectory(rootDir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(rootDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                SessionDto dto = mapper.readValue(file.toFile(), SessionDto.class);
                if (dto.projectId().equals(projectId)) {
                    result.add(dto.toSession());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list sessions for project " + projectId, e);
        }
        return result;
    }

    @Override
    public void delete(String sessionId) {
        try {
            Files.deleteIfExists(fileFor(sessionId));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete session " + sessionId, e);
        }
    }

    private Path fileFor(String sessionId) {
        return rootDir.resolve(sessionId + ".json");
    }
}
