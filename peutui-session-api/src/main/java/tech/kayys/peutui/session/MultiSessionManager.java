package tech.kayys.peutui.session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SessionManager} strategy tracking several concurrently open sessions,
 * with one designated as "current" (focused).
 */
public final class MultiSessionManager implements SessionManager {

    private final SessionStore store;
    private final Map<String, Session> open = new LinkedHashMap<>();
    private String currentId;

    public MultiSessionManager(SessionStore store) {
        this.store = store;
    }

    @Override
    public Session current() {
        if (currentId == null) {
            throw new IllegalStateException("No open sessions - call startNew() first");
        }
        return open.get(currentId);
    }

    @Override
    public Session startNew(String projectId, String agentId) {
        Session created = store.save(Session.create(projectId, agentId));
        open.put(created.id(), created);
        currentId = created.id();
        return created;
    }

    @Override
    public Session persist(Session updated) {
        Session saved = store.save(updated);
        open.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Session switchTo(String sessionId) {
        Session session = open.computeIfAbsent(sessionId, id -> store.load(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown session: " + id)));
        currentId = session.id();
        return session;
    }

    public void close(String sessionId) {
        open.remove(sessionId);
        if (sessionId.equals(currentId)) {
            currentId = open.isEmpty() ? null : open.keySet().iterator().next();
        }
    }

    @Override
    public List<Session> openSessions() {
        return List.copyOf(open.values());
    }
}
