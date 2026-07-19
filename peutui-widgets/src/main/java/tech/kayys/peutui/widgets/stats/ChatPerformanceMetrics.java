package tech.kayys.peutui.widgets.stats;

import java.util.Objects;

/**
 * Represents detailed performance metrics for a single chat response/stream.
 * Captures timing and throughput information similar to LLM inference
 * profiling.
 * 
 * @param streamUpdates        number of streaming updates/chunks received
 * @param durationMs           total duration in milliseconds
 * @param tokensPerSecond      generation speed in tokens per second
 * @param ttftMs               time to first token in milliseconds
 * @param loadTimeMs           initial model load time (if applicable)
 * @param promptEvalSpeed      prompt evaluation speed in tokens/sec
 * @param promptTokens         number of prompt/input tokens
 * @param promptEvalTimeMs     time spent evaluating prompt
 * @param generatedTokens      number of generated/output tokens
 * @param decodeSpeed          decoding speed in tokens/sec
 * @param decodeSteps          number of decode steps
 * @param engineTtftMs         engine-specific TTFT (model-ready time)
 * @param tokenLatencyMs       average latency per token
 * @param prefillTimeMs        time spent in prefill phase
 * @param decodeTimeMs         time spent in decode phase
 * @param samplingTimeMs       time spent in sampling
 * @param attentionTimeMs      time spent in attention (if tracked)
 * @param ffnTimeMs            time spent in feed-forward network (if tracked)
 * @param logitsTimeMs         time spent in logits processing
 * @param logitsCopyTimeMs     time spent copying logits
 * @param bottleneckType       identified bottleneck type (e.g., "decode",
 *                             "prefill")
 * @param bottleneckValueMs    bottleneck duration in milliseconds
 * @param bottleneckPercentage bottleneck percentage relative to total
 * @param sessionId            optional session identifier
 * @param modelId              optional model identifier
 */
public record ChatPerformanceMetrics(
        int streamUpdates,
        long durationMs,
        double tokensPerSecond,
        double ttftMs,
        long loadTimeMs,
        double promptEvalSpeed,
        int promptTokens,
        long promptEvalTimeMs,
        int generatedTokens,
        double decodeSpeed,
        int decodeSteps,
        double engineTtftMs,
        double tokenLatencyMs,
        double prefillTimeMs,
        double decodeTimeMs,
        double samplingTimeMs,
        double attentionTimeMs,
        double ffnTimeMs,
        double logitsTimeMs,
        double logitsCopyTimeMs,
        String bottleneckType,
        double bottleneckValueMs,
        double bottleneckPercentage,
        String sessionId,
        String modelId) {

    public ChatPerformanceMetrics {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        if (tokensPerSecond < 0) {
            throw new IllegalArgumentException("tokensPerSecond cannot be negative");
        }
        if (ttftMs < 0) {
            throw new IllegalArgumentException("ttftMs cannot be negative");
        }
        if (loadTimeMs < 0) {
            throw new IllegalArgumentException("loadTimeMs cannot be negative");
        }
        if (promptTokens < 0) {
            throw new IllegalArgumentException("promptTokens cannot be negative");
        }
        if (generatedTokens < 0) {
            throw new IllegalArgumentException("generatedTokens cannot be negative");
        }
    }

    /**
     * Builder for creating ChatPerformanceMetrics with sensible defaults.
     */
    public static class Builder {
        private int streamUpdates = 0;
        private long durationMs = 0;
        private double tokensPerSecond = 0.0;
        private double ttftMs = 0.0;
        private long loadTimeMs = 0;
        private double promptEvalSpeed = 0.0;
        private int promptTokens = 0;
        private long promptEvalTimeMs = 0;
        private int generatedTokens = 0;
        private double decodeSpeed = 0.0;
        private int decodeSteps = 0;
        private double engineTtftMs = 0.0;
        private double tokenLatencyMs = 0.0;
        private double prefillTimeMs = 0.0;
        private double decodeTimeMs = 0.0;
        private double samplingTimeMs = 0.0;
        private double attentionTimeMs = 0.0;
        private double ffnTimeMs = 0.0;
        private double logitsTimeMs = 0.0;
        private double logitsCopyTimeMs = 0.0;
        private String bottleneckType = "";
        private double bottleneckValueMs = 0.0;
        private double bottleneckPercentage = 0.0;
        private String sessionId;
        private String modelId;

        public Builder streamUpdates(int streamUpdates) {
            this.streamUpdates = streamUpdates;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder tokensPerSecond(double tokensPerSecond) {
            this.tokensPerSecond = tokensPerSecond;
            return this;
        }

        public Builder ttftMs(double ttftMs) {
            this.ttftMs = ttftMs;
            return this;
        }

        public Builder loadTimeMs(long loadTimeMs) {
            this.loadTimeMs = loadTimeMs;
            return this;
        }

        public Builder promptEvalSpeed(double promptEvalSpeed) {
            this.promptEvalSpeed = promptEvalSpeed;
            return this;
        }

        public Builder promptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder promptEvalTimeMs(long promptEvalTimeMs) {
            this.promptEvalTimeMs = promptEvalTimeMs;
            return this;
        }

        public Builder generatedTokens(int generatedTokens) {
            this.generatedTokens = generatedTokens;
            return this;
        }

        public Builder decodeSpeed(double decodeSpeed) {
            this.decodeSpeed = decodeSpeed;
            return this;
        }

        public Builder decodeSteps(int decodeSteps) {
            this.decodeSteps = decodeSteps;
            return this;
        }

        public Builder engineTtftMs(double engineTtftMs) {
            this.engineTtftMs = engineTtftMs;
            return this;
        }

        public Builder tokenLatencyMs(double tokenLatencyMs) {
            this.tokenLatencyMs = tokenLatencyMs;
            return this;
        }

        public Builder prefillTimeMs(double prefillTimeMs) {
            this.prefillTimeMs = prefillTimeMs;
            return this;
        }

        public Builder decodeTimeMs(double decodeTimeMs) {
            this.decodeTimeMs = decodeTimeMs;
            return this;
        }

        public Builder samplingTimeMs(double samplingTimeMs) {
            this.samplingTimeMs = samplingTimeMs;
            return this;
        }

        public Builder attentionTimeMs(double attentionTimeMs) {
            this.attentionTimeMs = attentionTimeMs;
            return this;
        }

        public Builder ffnTimeMs(double ffnTimeMs) {
            this.ffnTimeMs = ffnTimeMs;
            return this;
        }

        public Builder logitsTimeMs(double logitsTimeMs) {
            this.logitsTimeMs = logitsTimeMs;
            return this;
        }

        public Builder logitsCopyTimeMs(double logitsCopyTimeMs) {
            this.logitsCopyTimeMs = logitsCopyTimeMs;
            return this;
        }

        public Builder bottleneck(String type, double valueMs, double percentage) {
            this.bottleneckType = type;
            this.bottleneckValueMs = valueMs;
            this.bottleneckPercentage = percentage;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public ChatPerformanceMetrics build() {
            return new ChatPerformanceMetrics(
                    streamUpdates,
                    durationMs,
                    tokensPerSecond,
                    ttftMs,
                    loadTimeMs,
                    promptEvalSpeed,
                    promptTokens,
                    promptEvalTimeMs,
                    generatedTokens,
                    decodeSpeed,
                    decodeSteps,
                    engineTtftMs,
                    tokenLatencyMs,
                    prefillTimeMs,
                    decodeTimeMs,
                    samplingTimeMs,
                    attentionTimeMs,
                    ffnTimeMs,
                    logitsTimeMs,
                    logitsCopyTimeMs,
                    bottleneckType,
                    bottleneckValueMs,
                    bottleneckPercentage,
                    sessionId,
                    modelId);
        }
    }

    /**
     * Creates a new builder for ChatPerformanceMetrics.
     */
    public static Builder builder() {
        return new Builder();
    }
}
