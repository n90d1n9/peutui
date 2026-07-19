package tech.kayys.peutui.session;

import java.util.List;

/**
 * {@link SessionManager} strategy that keeps exactly one active session at a
 * time.
 */
public final class SingleSessionManager implements SessionManager {

    private final SessionStore store;
    private Session current;

    public SingleSessionManager(SessionStore store, Session initial) {
        this.store = store;
        this.current = store.save(initial);
    }

    @Override
    public Session current() {
        return current;
    }

    @Override
    public Session startNew(String projectId, String agentId) {
        current = store.save(Session.create(projectId, agentId));
        return current;
    }

    @Override
    public Session persist(Session updated) {
        current = store.save(updated);
        return current;
    }

    @Override
    public Session switchTo(String sessionId) {
        current = store.load(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
        return current;
    }

    @Override
    public List<Session> openSessions() {
        return List.of(current);
    }
}
