package tech.kayys.peutui.provider;

import java.util.concurrent.Flow;

/**
 * Strategy interface implemented once per backend model provider (Anthropic,
 * an OpenAI-compatible endpoint, a local Gollek/llama.cpp/Ollama server,
 * etc). Agents depend on this interface, never on a concrete HTTP client, so
 * a host application can register any number of providers and switch or mix
 * them via {@link ProviderRegistry} without touching agent code.
 */
public interface ModelProvider {

    String id();

    String displayName();

    /**
     * Streams a chat completion; the returned publisher emits zero or more deltas
     * followed by one Done/Error.
     */
    Flow.Publisher<ProviderEvent> streamChat(ProviderRequest request);

    /**
     * Whether this provider is currently reachable/configured (used by failover
     * strategies).
     */
    default boolean isAvailable() {
        return true;
    }
}
