package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.provider.FailoverProviderSelectionStrategy;
import tech.kayys.peutui.provider.ModelProvider;
import tech.kayys.peutui.provider.ProviderRegistry;
import tech.kayys.peutui.provider.ProviderSelectionStrategy;
import tech.kayys.peutui.provider.RoundRobinProviderSelectionStrategy;
import tech.kayys.peutui.provider.SingleProviderSelectionStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Collects every CDI bean implementing {@link ModelProvider} into a
 * {@link ProviderRegistry}, then produces the {@link ProviderSelectionStrategy}
 * matching {@code peutui.provider.selection}.
 */
@ApplicationScoped
public final class ProviderRegistryProducer {

    @Inject
    PeutuiConfig config;

    @Inject
    Instance<ModelProvider> providers;

    @Produces
    @ApplicationScoped
    public ProviderRegistry providerRegistry() {
        ProviderRegistry registry = new ProviderRegistry();
        for (ModelProvider provider : providers) {
            registry.register(provider);
        }
        return registry;
    }

    @Produces
    @ApplicationScoped
    public ProviderSelectionStrategy providerSelectionStrategy() {
        return switch (config.provider().selection()) {
            case SINGLE -> new SingleProviderSelectionStrategy(
                    config.provider().defaultProviderId()
                            .orElseThrow(() -> new IllegalStateException(
                                    "peutui.provider.default-provider-id is required when selection=single")));
            case ROUND_ROBIN -> new RoundRobinProviderSelectionStrategy();
            case FAILOVER -> new FailoverProviderSelectionStrategy(
                    config.provider().failoverOrder()
                            .orElseThrow(() -> new IllegalStateException(
                                    "peutui.provider.failover-order is required when selection=failover")));
        };
    }
}
