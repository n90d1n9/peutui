package tech.kayys.peutui.agent;

import java.util.concurrent.Flow;

/**
 * The core extension point of this library: anything that can take a
 * conversation turn and stream back {@link AgentEvent}s. Peutui is
 * deliberately silent about *how* an Agent talks to a model - that's the
 * job of the {@code peutui-provider-api} strategy layer, which a concrete
 * Agent implementation typically composes internally.
 */
public interface Agent {

    AgentDescriptor descriptor();

    /**
     * Starts a turn given the current context, returning a publisher of
     * incremental {@link AgentEvent}s. Implementations should publish
     * exactly one terminal event ({@code TurnCompleted} or {@code TurnFailed}).
     */
    Flow.Publisher<AgentEvent> converse(AgentContext context);

    default String id() {
        return descriptor().id();
    }
}
