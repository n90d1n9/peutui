package tech.kayys.peutui.agent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link AgentRouter} that cycles through all registered agents in turn
 * order.
 */
public final class RoundRobinAgentRouter implements AgentRouter {

    private final AtomicInteger cursor = new AtomicInteger(0);

    @Override
    public List<Agent> route(AgentContext context, List<Agent> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        int index = Math.floorMod(cursor.getAndIncrement(), candidates.size());
        return List.of(candidates.get(index));
    }
}
