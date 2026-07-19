package tech.kayys.peutui.agent;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestration strategy for multi-agent setups: uses an {@link AgentRouter}
 * to pick one or more candidate agents for the turn, then fans the turn out
 * to each chosen agent concurrently and merges their event streams into a
 * single publisher (events are tagged with {@code agentId} so subscribers -
 * typically a chat UI - can attribute output to the right agent).
 */
public final class MultiAgentOrchestrationStrategy implements AgentOrchestrationStrategy {

    private final AgentRegistry registry;
    private final AgentRouter router;

    public MultiAgentOrchestrationStrategy(AgentRegistry registry, AgentRouter router) {
        this.registry = registry;
        this.router = router;
    }

    @Override
    public Flow.Publisher<AgentEvent> dispatch(AgentContext context) {
        List<Agent> chosen = router.route(context, registry.all());
        if (chosen.isEmpty()) {
            throw new IllegalStateException("Router selected no agent for this turn");
        }
        if (chosen.size() == 1) {
            return chosen.get(0).converse(context);
        }
        return mergeAll(chosen, context);
    }

    private Flow.Publisher<AgentEvent> mergeAll(List<Agent> agents, AgentContext context) {
        SubmissionPublisher<AgentEvent> merged = new SubmissionPublisher<>();
        AtomicInteger remaining = new AtomicInteger(agents.size());
        for (Agent agent : agents) {
            agent.converse(context).subscribe(new Flow.Subscriber<>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(AgentEvent item) {
                    merged.submit(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    merged.submit(new AgentEvent.TurnFailed(agent.id(), String.valueOf(throwable.getMessage())));
                    finishOne();
                }

                @Override
                public void onComplete() {
                    finishOne();
                }

                private void finishOne() {
                    if (remaining.decrementAndGet() == 0) {
                        merged.close();
                    }
                }
            });
        }
        return merged;
    }
}
