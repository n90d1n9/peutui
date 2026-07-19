package tech.kayys.peutui.agent;

import java.util.List;

/**
 * Strategy for picking which agent(s) should handle a turn in a multi-agent
 * setup, e.g. by matching declared {@link AgentCapability} tags against the
 * user's request, by explicit {@code @agent-name} addressing, or by a
 * supervisor model classifying intent.
 */
public interface AgentRouter {

    /**
     * Returns the ordered list of agents that should be considered for this turn,
     * most-preferred first.
     */
    List<Agent> route(AgentContext context, List<Agent> candidates);
}
