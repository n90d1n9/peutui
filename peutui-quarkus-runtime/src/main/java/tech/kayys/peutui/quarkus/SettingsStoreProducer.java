package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.settings.LayeredSettingsResolver;
import tech.kayys.peutui.settings.SettingsScope;
import tech.kayys.peutui.settings.SettingsStore;
import tech.kayys.peutui.settings.StorageBackedSettingsStore;
import tech.kayys.peutui.storage.StorageBackend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Produces the {@link SettingsStore} (storage-backed per
 * {@code peutui.settings-storage.*})
 * and a {@link LayeredSettingsResolver} wired to use it uniformly across
 * every {@link SettingsScope} - GLOBAL/USER scopes rarely need a different
 * backend than PROJECT/SESSION scopes, but a host app can still produce its
 * own resolver with a per-scope {@code Map} if it needs to split them.
 */
@ApplicationScoped
public final class SettingsStoreProducer {

    @Inject
    @SettingsStorage
    StorageBackend settingsStorageBackend;

    @Produces
    @ApplicationScoped
    public SettingsStore settingsStore() {
        return new StorageBackedSettingsStore(settingsStorageBackend);
    }

    @Produces
    @ApplicationScoped
    public LayeredSettingsResolver settingsResolver(SettingsStore store) {
        Map<SettingsScope, SettingsStore> byScope = new EnumMap<>(SettingsScope.class);
        for (SettingsScope scope : SettingsScope.values()) {
            byScope.put(scope, store);
        }
        return new LayeredSettingsResolver(byScope);
    }
}
