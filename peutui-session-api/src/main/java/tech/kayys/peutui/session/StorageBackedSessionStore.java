package tech.kayys.peutui.session;

import tech.kayys.peutui.storage.StorageBackend;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link SessionStore} strategy that layers session persistence on top of
 * any {@link StorageBackend} - local file, local database, or cloud -
 * chosen by the host application. This is the bridge that lets "where do
 * sessions live" be a pure storage-mode configuration decision rather than
 * a different code path per backend.
 */
public final class StorageBackedSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "sessions/";

    private final StorageBackend backend;
    private final ObjectMapper mapper;

    public StorageBackedSessionStore(StorageBackend backend) {
        this.backend = backend;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Session save(Session session) {
        try {
            byte[] json = mapper.writeValueAsBytes(SessionDto.from(session));
            backend.put(keyFor(session.id()), json);
            return session;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize session " + session.id(), e);
        }
    }

    @Override
    public Optional<Session> load(String sessionId) {
        return backend.get(keyFor(sessionId)).map(this::deserialize);
    }

    @Override
    public List<Session> listByProject(String projectId) {
        List<Session> result = new ArrayList<>();
        for (String key : backend.listKeys(KEY_PREFIX)) {
            backend.get(key).map(this::deserialize)
                    .filter(session -> session.projectId().equals(projectId))
                    .ifPresent(result::add);
        }
        return result;
    }

    @Override
    public void delete(String sessionId) {
        backend.delete(keyFor(sessionId));
    }

    private Session deserialize(byte[] json) {
        try {
            return mapper.readValue(new String(json, StandardCharsets.UTF_8), SessionDto.class).toSession();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to deserialize session data", e);
        }
    }

    private static String keyFor(String sessionId) {
        return KEY_PREFIX + sessionId + ".json";
    }
}
