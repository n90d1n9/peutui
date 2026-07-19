package tech.kayys.peutui.provider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cycles through all registered providers in turn order, e.g. to spread load or
 * cost across accounts.
 */
public final class RoundRobinProviderSelectionStrategy implements ProviderSelectionStrategy {

    private final AtomicInteger cursor = new AtomicInteger(0);

    @Override
    public ModelProvider select(ProviderRegistry registry, ProviderRequest request) {
        List<ModelProvider> all = registry.all();
        if (all.isEmpty()) {
            throw new IllegalStateException("No providers registered");
        }
        int index = Math.floorMod(cursor.getAndIncrement(), all.size());
        return all.get(index);
    }
}
