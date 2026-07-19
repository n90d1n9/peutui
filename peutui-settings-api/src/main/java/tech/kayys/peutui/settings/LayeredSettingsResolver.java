package tech.kayys.peutui.settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Merges the {@link SettingsScope} layers, each potentially backed by a
 * different {@link SettingsStore} strategy, into a single "effective value"
 * view. Precedence is highest scope wins: SESSION overrides PROJECT
 * overrides USER overrides GLOBAL, and any scope missing from the given
 * {@link ScopeContext} is simply skipped.
 */
public final class LayeredSettingsResolver {

    private final Map<SettingsScope, SettingsStore> storesByScope;

    public LayeredSettingsResolver(Map<SettingsScope, SettingsStore> storesByScope) {
        this.storesByScope = Map.copyOf(storesByScope);
    }

    /**
     * Resolves the effective value of {@code key}, checking scopes from highest to
     * lowest precedence.
     */
    public Optional<String> resolve(String key, ScopeContext context) {
        SettingsScope[] scopes = SettingsScope.ascendingPrecedence();
        for (int i = scopes.length - 1; i >= 0; i--) {
            SettingsScope scope = scopes[i];
            if (!context.has(scope)) {
                continue;
            }
            SettingsStore store = storesByScope.get(scope);
            if (store == null) {
                continue;
            }
            Optional<String> value = store.get(context.idFor(scope), key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the effective value for every key visible from any applicable scope,
     * already merged by precedence.
     */
    public Map<String, String> resolveAll(ScopeContext context) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (SettingsScope scope : SettingsScope.ascendingPrecedence()) {
            if (!context.has(scope)) {
                continue;
            }
            SettingsStore store = storesByScope.get(scope);
            if (store == null) {
                continue;
            }
            merged.putAll(store.getAll(context.idFor(scope))); // later (higher-precedence) scopes overwrite
        }
        return merged;
    }

    /** Writes {@code key=value} directly into the given scope's store. */
    public void setAt(SettingsScope scope, String key, String value, ScopeContext context) {
        SettingsStore store = storesByScope.get(scope);
        if (store == null) {
            throw new IllegalStateException("No SettingsStore configured for scope " + scope);
        }
        store.set(context.idFor(scope), key, value);
    }
}
