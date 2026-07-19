package tech.kayys.peutui.provider;

/**
 * Always resolves to one fixed, explicitly configured provider. The default for
 * single-provider setups.
 */
public final class SingleProviderSelectionStrategy implements ProviderSelectionStrategy {

    private final String providerId;

    public SingleProviderSelectionStrategy(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public ModelProvider select(ProviderRegistry registry, ProviderRequest request) {
        return registry.find(providerId)
                .orElseThrow(() -> new IllegalStateException("Provider '" + providerId + "' is not registered"));
    }
}
