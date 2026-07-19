package tech.kayys.peutui.widgets.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents token usage statistics for a chat session or time period.
 * 
 * @param totalTokens  total number of tokens used (input + output)
 * @param inputTokens  number of input/prompt tokens
 * @param outputTokens number of output/completion tokens
 * @param cost         estimated cost in USD (optional, can be 0.0 if not
 *                     tracked)
 * @param timestamp    when this usage was recorded
 * @param sessionId    optional session identifier
 * @param modelId      optional model identifier
 */
public record TokenUsageStats(
        long totalTokens,
        long inputTokens,
        long outputTokens,
        double cost,
        LocalDateTime timestamp,
        String sessionId,
        String modelId) {

    public TokenUsageStats {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (totalTokens < 0) {
            throw new IllegalArgumentException("totalTokens cannot be negative");
        }
        if (inputTokens < 0) {
            throw new IllegalArgumentException("inputTokens cannot be negative");
        }
        if (outputTokens < 0) {
            throw new IllegalArgumentException("outputTokens cannot be negative");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost cannot be negative");
        }

        // Validate consistency if all values provided
        if (totalTokens == 0 && (inputTokens > 0 || outputTokens > 0)) {
            throw new IllegalArgumentException("totalTokens should equal inputTokens + outputTokens when non-zero");
        }
    }

    /**
     * Creates a TokenUsageStats with auto-calculated total.
     */
    public static TokenUsageStats of(long inputTokens, long outputTokens, LocalDateTime timestamp) {
        return of(inputTokens, outputTokens, 0.0, timestamp, null, null);
    }

    /**
     * Creates a TokenUsageStats with cost calculation.
     */
    public static TokenUsageStats of(long inputTokens, long outputTokens, double cost, LocalDateTime timestamp) {
        return of(inputTokens, outputTokens, cost, timestamp, null, null);
    }

    /**
     * Full constructor with auto-calculated total.
     */
    public static TokenUsageStats of(
            long inputTokens,
            long outputTokens,
            double cost,
            LocalDateTime timestamp,
            String sessionId,
            String modelId) {
        return new TokenUsageStats(
                inputTokens + outputTokens,
                inputTokens,
                outputTokens,
                cost,
                timestamp,
                sessionId,
                modelId);
    }

    /**
     * Gets the date portion of the timestamp.
     */
    public LocalDate date() {
        return timestamp.toLocalDate();
    }

    /**
     * Checks if this usage is from today.
     */
    public boolean isToday() {
        return date().equals(LocalDate.now());
    }

    /**
     * Checks if this usage is from the current month.
     */
    public boolean isThisMonth() {
        LocalDate now = LocalDate.now();
        LocalDate date = date();
        return date.getMonth().equals(now.getMonth()) && date.getYear() == now.getYear();
    }
}
