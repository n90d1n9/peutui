package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.agent.Agent;
import tech.kayys.peutui.agent.AgentOrchestrationStrategy;
import tech.kayys.peutui.agent.AgentRegistry;
import tech.kayys.peutui.agent.CapabilityAgentRouter;
import tech.kayys.peutui.agent.AgentCapability;
import tech.kayys.peutui.agent.MultiAgentOrchestrationStrategy;
import tech.kayys.peutui.agent.SingleAgentOrchestrationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Collects every CDI bean implementing {@link Agent} into an
 * {@link AgentRegistry}, then produces the {@link AgentOrchestrationStrategy}
 * matching {@code peutui.agent.mode}: a single fixed/default agent, or a
 * capability-based router across all registered agents.
 *
 * <p>
 * Registering a new agent for a host application is therefore just
 * writing an {@code @ApplicationScoped} bean implementing {@code Agent} -
 * no wiring changes needed here.
 */
@ApplicationScoped
public final class AgentOrchestrationProducer {

    @Inject
    PeutuiConfig config;

    @Inject
    Instance<Agent> agents;

    @Produces
    @ApplicationScoped
    public AgentRegistry agentRegistry() {
        AgentRegistry registry = new AgentRegistry();
        for (Agent agent : agents) {
            registry.register(agent);
        }
        return registry;
    }

    @Produces
    @ApplicationScoped
    public AgentOrchestrationStrategy agentOrchestrationStrategy(AgentRegistry registry) {
        return switch (config.agent().mode()) {
            case SINGLE -> new SingleAgentOrchestrationStrategy(registry, config.agent().defaultAgentId().orElse(null));
            case MULTI -> new MultiAgentOrchestrationStrategy(registry,
                    new CapabilityAgentRouter(context -> AgentCapability.GENERAL));
        };
    }
}
