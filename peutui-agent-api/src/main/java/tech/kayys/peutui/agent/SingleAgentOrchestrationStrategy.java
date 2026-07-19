package tech.kayys.peutui.agent;

import java.util.List;
import java.util.concurrent.Flow;

/**
 * The simplest orchestration strategy: every turn goes to exactly one agent
 * - either the only one registered, or an explicitly configured default.
 * Fails fast if the registry is empty or, in a multi-agent registry, if no
 * default has been configured.
 */
public final class SingleAgentOrchestrationStrategy implements AgentOrchestrationStrategy {

    private final AgentRegistry registry;
    private final String defaultAgentId;

    public SingleAgentOrchestrationStrategy(AgentRegistry registry) {
        this(registry, null);
    }

    public SingleAgentOrchestrationStrategy(AgentRegistry registry, String defaultAgentId) {
        this.registry = registry;
        this.defaultAgentId = defaultAgentId;
    }

    @Override
    public Flow.Publisher<AgentEvent> dispatch(AgentContext context) {
        Agent agent = resolveAgent();
        return agent.converse(context);
    }

    private Agent resolveAgent() {
        List<Agent> all = registry.all();
        if (all.isEmpty()) {
            throw new IllegalStateException("No agents registered");
        }
        if (defaultAgentId != null) {
            return registry.find(defaultAgentId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Default agent '" + defaultAgentId + "' is not registered"));
        }
        if (all.size() > 1) {
            throw new IllegalStateException(
                    "SingleAgentOrchestrationStrategy requires exactly one registered agent, or an explicit defaultAgentId; found "
                            + all.size());
        }
        return all.get(0);
    }
}
