package tech.kayys.peutui.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the set of {@link Agent}s available to the running application.
 * Works identically whether exactly one agent is registered (single-agent
 * mode) or many (multi-agent mode) - it is the
 * {@link AgentOrchestrationStrategy}
 * that decides how a given turn is dispatched across whatever is registered
 * here.
 */
public final class AgentRegistry {

    private final Map<String, Agent> agentsById = new LinkedHashMap<>();

    public AgentRegistry register(Agent agent) {
        agentsById.put(agent.id(), agent);
        return this;
    }

    public Optional<Agent> find(String agentId) {
        return Optional.ofNullable(agentsById.get(agentId));
    }

    public List<Agent> all() {
        return List.copyOf(agentsById.values());
    }

    public boolean isMultiAgent() {
        return agentsById.size() > 1;
    }

    public int size() {
        return agentsById.size();
    }
}
