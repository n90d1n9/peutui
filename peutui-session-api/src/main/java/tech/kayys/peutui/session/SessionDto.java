package tech.kayys.peutui.session;

import tech.kayys.peutui.agent.AgentMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Plain, Jackson-friendly mirror of {@link Session} used only at the
 * serialization boundary (file/db/cloud stores), keeping the domain class
 * itself free of persistence-framework annotations.
 */
public record SessionDto(String id, String projectId, String agentId, Instant createdAt, Instant updatedAt,
        List<AgentMessage> messages, Map<String, String> state) {

    public static SessionDto from(Session session) {
        return new SessionDto(session.id(), session.projectId(), session.agentId(), session.createdAt(),
                session.updatedAt(), session.messages(), session.state());
    }

    public Session toSession() {
        return new Session(id, projectId, agentId, createdAt, updatedAt, messages, state);
    }
}
