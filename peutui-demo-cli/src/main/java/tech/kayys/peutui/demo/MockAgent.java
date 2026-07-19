package tech.kayys.peutui.demo;

import tech.kayys.peutui.agent.Agent;
import tech.kayys.peutui.agent.AgentCapability;
import tech.kayys.peutui.agent.AgentContext;
import tech.kayys.peutui.agent.AgentDescriptor;
import tech.kayys.peutui.agent.AgentEvent;
import tech.kayys.peutui.agent.AgentMessage;
import tech.kayys.peutui.provider.ProviderEvent;
import tech.kayys.peutui.provider.ProviderMessage;
import tech.kayys.peutui.provider.ProviderRegistry;
import tech.kayys.peutui.provider.ProviderRequest;
import tech.kayys.peutui.provider.ProviderSelectionStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Demonstrates the intended shape of a real {@link Agent}: it does not talk
 * to a model directly, it asks the injected {@link ProviderRegistry} /
 * {@link ProviderSelectionStrategy} for whichever {@code ModelProvider} is
 * configured, and translates the resulting {@link ProviderEvent} stream into
 * {@link AgentEvent}s.
 */
@ApplicationScoped
public final class MockAgent implements Agent {

    @Inject
    ProviderRegistry providerRegistry;

    @Inject
    ProviderSelectionStrategy providerSelectionStrategy;

    @Override
    public AgentDescriptor descriptor() {
        return new AgentDescriptor("assistant", "Assistant", "General-purpose demo agent",
                List.of(AgentCapability.GENERAL));
    }

    @Override
    public Flow.Publisher<AgentEvent> converse(AgentContext context) {
        ProviderRequest request = new ProviderRequest("mock-model", toProviderMessages(context));
        var provider = providerSelectionStrategy.select(providerRegistry, request);

        SubmissionPublisher<AgentEvent> out = new SubmissionPublisher<>();
        StringBuilder accumulated = new StringBuilder();
        provider.streamChat(request).subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ProviderEvent item) {
                switch (item) {
                    case ProviderEvent.TextDelta delta -> {
                        accumulated.append(delta.text());
                        out.submit(new AgentEvent.TextDelta(id(), delta.text()));
                    }
                    case ProviderEvent.ToolUse toolUse ->
                        out.submit(new AgentEvent.ToolCallStarted(id(), toolUse.toolName(), toolUse.toolCallId(),
                                toolUse.argumentsJson()));
                    case ProviderEvent.Done done ->
                        out.submit(new AgentEvent.TurnCompleted(id(),
                                AgentMessage.assistant(id(), accumulated.toString())));
                    case ProviderEvent.Error error -> out.submit(new AgentEvent.TurnFailed(id(), error.message()));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                out.submit(new AgentEvent.TurnFailed(id(), String.valueOf(throwable.getMessage())));
                out.close();
            }

            @Override
            public void onComplete() {
                out.close();
            }
        });
        return out;
    }

    private List<ProviderMessage> toProviderMessages(AgentContext context) {
        List<ProviderMessage> messages = new ArrayList<>();
        for (AgentMessage message : context.history()) {
            messages.add(new ProviderMessage(message.role().name().toLowerCase(), message.content()));
        }
        return messages;
    }
}
