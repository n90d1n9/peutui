package tech.kayys.peutui.widgets.streaming;

/**
 * Represents a streaming token from an AI model.
 * Used for incremental rendering of AI responses.
 */
public record StreamingToken(
        String content,
        boolean isComplete,
        String finishReason,
        long timestampMs) {
    public StreamingToken(String content) {
        this(content, false, null, System.currentTimeMillis());
    }

    public static StreamingToken complete(String content, String finishReason) {
        return new StreamingToken(content, true, finishReason, System.currentTimeMillis());
    }
}
