package tech.kayys.peutui.agent;

import java.time.Instant;
import java.util.Map;

/**
 * A single message exchanged between the user, an agent, and any tools it
 * invokes.
 */
public record AgentMessage(String id, MessageRole role, String content, String agentId, Instant timestamp,
        Map<String, String> metadata) {

    public static AgentMessage user(String content) {
        return new AgentMessage(java.util.UUID.randomUUID().toString(), MessageRole.USER, content, null, Instant.now(),
                Map.of());
    }

    public static AgentMessage assistant(String agentId, String content) {
        return new AgentMessage(java.util.UUID.randomUUID().toString(), MessageRole.ASSISTANT, content, agentId,
                Instant.now(), Map.of());
    }
}
