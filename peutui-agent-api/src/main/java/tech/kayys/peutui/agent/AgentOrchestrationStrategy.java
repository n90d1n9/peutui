package tech.kayys.peutui.agent;

import java.util.concurrent.Flow;

/**
 * Top-level strategy for how a conversation turn is dispatched across the
 * {@link AgentRegistry}. Two default implementations are provided -
 * {@link SingleAgentOrchestrationStrategy} and
 * {@link MultiAgentOrchestrationStrategy} - but hosts are free to implement
 * their own (e.g. a supervisor/worker pipeline that fans a turn out to
 * several agents and merges results).
 */
public interface AgentOrchestrationStrategy {

    /**
     * Dispatches a turn and returns a publisher of the resulting stream of events,
     * possibly interleaved from several agents.
     */
    Flow.Publisher<AgentEvent> dispatch(AgentContext context);
}
