package tech.kayys.peutui.provider;

/** A streamed chunk of a provider's response. */
public sealed interface ProviderEvent {

    record TextDelta(String text) implements ProviderEvent {
    }

    record ToolUse(String toolName, String toolCallId, String argumentsJson) implements ProviderEvent {
    }

    record Done(String stopReason, int inputTokens, int outputTokens) implements ProviderEvent {
    }

    record Error(String message) implements ProviderEvent {
    }
}
