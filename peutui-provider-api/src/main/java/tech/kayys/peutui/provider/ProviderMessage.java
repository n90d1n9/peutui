package tech.kayys.peutui.provider;

/** A single role/content pair sent as part of a {@link ProviderRequest}. */
public record ProviderMessage(String role, String content) {
    public static ProviderMessage user(String content) {
        return new ProviderMessage("user", content);
    }

    public static ProviderMessage assistant(String content) {
        return new ProviderMessage("assistant", content);
    }

    public static ProviderMessage system(String content) {
        return new ProviderMessage("system", content);
    }
}
