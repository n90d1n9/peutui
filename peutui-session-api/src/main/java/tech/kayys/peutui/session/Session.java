package tech.kayys.peutui.session;

import tech.kayys.peutui.agent.AgentMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A single, independently persisted conversation: an ordered message
 * history plus free-form state, scoped to a project and (usually) an agent.
 * Immutable snapshot semantics - mutating helpers return a new instance so
 * {@link SessionStore} implementations can treat "save" as an atomic
 * replace.
 */
public final class Session {

    private final String id;
    private final String projectId;
    private final String agentId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<AgentMessage> messages;
    private final Map<String, String> state;

    public Session(String id, String projectId, String agentId, Instant createdAt, Instant updatedAt,
            List<AgentMessage> messages, Map<String, String> state) {
        this.id = id;
        this.projectId = projectId;
        this.agentId = agentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = List.copyOf(messages);
        this.state = Map.copyOf(state);
    }

    public static Session create(String projectId, String agentId) {
        Instant now = Instant.now();
        return new Session(java.util.UUID.randomUUID().toString(), projectId, agentId, now, now, List.of(), Map.of());
    }

    public Session withMessage(AgentMessage message) {
        List<AgentMessage> updated = new ArrayList<>(messages);
        updated.add(message);
        return new Session(id, projectId, agentId, createdAt, Instant.now(), updated, state);
    }

    public Session withState(String key, String value) {
        Map<String, String> updated = new java.util.LinkedHashMap<>(state);
        updated.put(key, value);
        return new Session(id, projectId, agentId, createdAt, Instant.now(), messages, updated);
    }

    public String id() {
        return id;
    }

    public String projectId() {
        return projectId;
    }

    public String agentId() {
        return agentId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<AgentMessage> messages() {
        return messages;
    }

    public Map<String, String> state() {
        return state;
    }
}
