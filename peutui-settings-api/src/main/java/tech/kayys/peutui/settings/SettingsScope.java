package tech.kayys.peutui.settings;

/**
 * A layer in the settings precedence chain, ordered from lowest to highest
 * priority. A value set at {@code SESSION} overrides the same key set at
 * {@code PROJECT}, which overrides {@code USER}, which overrides
 * {@code GLOBAL}.
 */
public enum SettingsScope {
    GLOBAL,
    USER,
    PROJECT,
    SESSION;

    /** Scopes in ascending precedence order (lowest priority first). */
    public static SettingsScope[] ascendingPrecedence() {
        return values();
    }
}
