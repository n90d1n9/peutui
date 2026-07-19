package tech.kayys.peutui.provider;

import java.util.List;

/**
 * Tries registered providers in a fixed preference order, skipping any that
 * report themselves unavailable via {@link ModelProvider#isAvailable()}.
 * Useful for a "primary cloud provider, local model as fallback" setup.
 */
public final class FailoverProviderSelectionStrategy implements ProviderSelectionStrategy {

    private final List<String> preferenceOrder;

    public FailoverProviderSelectionStrategy(List<String> preferenceOrder) {
        this.preferenceOrder = List.copyOf(preferenceOrder);
    }

    @Override
    public ModelProvider select(ProviderRegistry registry, ProviderRequest request) {
        for (String id : preferenceOrder) {
            var provider = registry.find(id);
            if (provider.isPresent() && provider.get().isAvailable()) {
                return provider.get();
            }
        }
        return registry.all().stream()
                .filter(ModelProvider::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available provider found"));
    }
}
