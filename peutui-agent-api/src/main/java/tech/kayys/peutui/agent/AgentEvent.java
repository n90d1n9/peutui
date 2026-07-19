package tech.kayys.peutui.agent;

/**
 * A streamed unit of an agent's response, mirroring how modern LLM agents
 * emit incremental output: text deltas, tool invocations/results, and a
 * final completion marker.
 */
public sealed interface AgentEvent {

    record TextDelta(String agentId, String text) implements AgentEvent {
    }

    record ToolCallStarted(String agentId, String toolName, String toolCallId, String argumentsJson)
            implements AgentEvent {
    }

    record ToolCallCompleted(String agentId, String toolCallId, String resultJson, boolean isError)
            implements AgentEvent {
    }

    record TurnCompleted(String agentId, AgentMessage finalMessage) implements AgentEvent {
    }

    record TurnFailed(String agentId, String reason) implements AgentEvent {
    }
}
