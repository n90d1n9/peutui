package tech.kayys.peutui.provider;

import java.util.List;
import java.util.Map;

/**
 * Model-agnostic chat completion request. Provider implementations translate
 * this into their wire format.
 */
public record ProviderRequest(String model, List<ProviderMessage> messages, double temperature, int maxTokens,
        Map<String, String> extraParams) {

    public ProviderRequest(String model, List<ProviderMessage> messages) {
        this(model, messages, 1.0, 4096, Map.of());
    }
}
