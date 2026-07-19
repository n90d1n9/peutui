package tech.kayys.peutui.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds every {@link ModelProvider} the running application knows about, single
 * or many.
 */
public final class ProviderRegistry {

    private final Map<String, ModelProvider> providersById = new LinkedHashMap<>();

    public ProviderRegistry register(ModelProvider provider) {
        providersById.put(provider.id(), provider);
        return this;
    }

    public Optional<ModelProvider> find(String id) {
        return Optional.ofNullable(providersById.get(id));
    }

    public List<ModelProvider> all() {
        return List.copyOf(providersById.values());
    }

    public boolean isMultiProvider() {
        return providersById.size() > 1;
    }
}
