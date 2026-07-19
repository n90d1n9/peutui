package tech.kayys.peutui.provider;

/**
 * Strategy for picking which registered {@link ModelProvider} should handle
 * a given request. Distinct from {@code AgentRouter}: this operates one
 * level lower, letting the same agent be backed by different providers
 * depending on load, cost, or availability.
 */
public interface ProviderSelectionStrategy {

    ModelProvider select(ProviderRegistry registry, ProviderRequest request);
}
