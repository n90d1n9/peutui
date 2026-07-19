package tech.kayys.peutui.settings;

import java.util.EnumMap;
import java.util.Map;

/**
 * The concrete scope-instance id to use for each {@link SettingsScope} when
 * resolving a value (e.g. which project, which session).
 */
public final class ScopeContext {

    private final Map<SettingsScope, String> idsByScope;

    private ScopeContext(Map<SettingsScope, String> idsByScope) {
        this.idsByScope = idsByScope;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String idFor(SettingsScope scope) {
        return idsByScope.get(scope);
    }

    public boolean has(SettingsScope scope) {
        return idsByScope.containsKey(scope);
    }

    public static final class Builder {
        private final Map<SettingsScope, String> ids = new EnumMap<>(SettingsScope.class);

        public Builder global() {
            ids.put(SettingsScope.GLOBAL, "global");
            return this;
        }

        public Builder user(String userId) {
            ids.put(SettingsScope.USER, userId);
            return this;
        }

        public Builder project(String projectId) {
            ids.put(SettingsScope.PROJECT, projectId);
            return this;
        }

        public Builder session(String sessionId) {
            ids.put(SettingsScope.SESSION, sessionId);
            return this;
        }

        public ScopeContext build() {
            return new ScopeContext(ids);
        }
    }
}
