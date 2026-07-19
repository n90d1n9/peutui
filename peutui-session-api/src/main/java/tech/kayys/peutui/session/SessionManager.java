package tech.kayys.peutui.session;

import java.util.List;

/**
 * Strategy over how many sessions are "live" in the running application at
 * once. {@link SingleSessionManager} pins one active session at a time
 * (a typical single-pane chat CLI); {@link MultiSessionManager} tracks a
 * set of concurrently open sessions (a tabbed/split-pane UI). Both delegate
 * actual persistence to the injected {@link SessionStore}.
 */
public interface SessionManager {

    Session current();

    /**
     * Starts a brand-new session for the given project/agent and makes it current.
     */
    Session startNew(String projectId, String agentId);

    /** Persists the current session's latest state. */
    Session persist(Session updated);

    /**
     * Switches "current" to an existing session id, loading it from the store if
     * necessary.
     */
    Session switchTo(String sessionId);

    List<Session> openSessions();

    default boolean isMultiSession() {
        return openSessions().size() > 1;
    }
}
