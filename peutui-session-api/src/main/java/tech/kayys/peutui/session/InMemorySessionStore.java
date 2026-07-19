package tech.kayys.peutui.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SessionStore} strategy that keeps everything in a concurrent in-memory
 * map. Not persisted across process restarts.
 */
public final class InMemorySessionStore implements SessionStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session save(Session session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<Session> load(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public List<Session> listByProject(String projectId) {
        Map<String, Session> matched = new LinkedHashMap<>();
        for (Session s : sessions.values()) {
            if (s.projectId().equals(projectId)) {
                matched.put(s.id(), s);
            }
        }
        return List.copyOf(matched.values());
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}
