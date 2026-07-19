package tech.kayys.peutui.agent;

import java.util.List;
import java.util.function.Function;

/**
 * Routes a turn to whichever registered agent's declared capabilities best
 * match a capability derived from the latest user message, falling back to
 * the first agent tagged {@link AgentCapability#GENERAL} (or the first
 * candidate overall) if nothing matches.
 */
public final class CapabilityAgentRouter implements AgentRouter {

    private final Function<AgentContext, AgentCapability> capabilityClassifier;

    public CapabilityAgentRouter(Function<AgentContext, AgentCapability> capabilityClassifier) {
        this.capabilityClassifier = capabilityClassifier;
    }

    @Override
    public List<Agent> route(AgentContext context, List<Agent> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        AgentCapability desired = capabilityClassifier.apply(context);
        return candidates.stream()
                .filter(agent -> agent.descriptor().capabilities().contains(desired))
                .findFirst()
                .map(List::of)
                .orElseGet(() -> candidates.stream()
                        .filter(agent -> agent.descriptor().capabilities().contains(AgentCapability.GENERAL))
                        .findFirst()
                        .map(List::of)
                        .orElse(List.of(candidates.get(0))));
    }
}
