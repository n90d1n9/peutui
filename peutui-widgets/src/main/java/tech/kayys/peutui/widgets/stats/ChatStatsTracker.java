package tech.kayys.peutui.widgets.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Abstract base class for tracking and aggregating chat statistics.
 * Provides foundation for daily/monthly token usage and performance metrics.
 * 
 * This class is designed to be extended by developers to add custom
 * aggregation logic, persistence, or integration with analytics services.
 */
public abstract class ChatStatsTracker {

    protected final Map<String, List<TokenUsageStats>> sessionUsage = new ConcurrentHashMap<>();
    protected final Map<String, List<ChatPerformanceMetrics>> sessionMetrics = new ConcurrentHashMap<>();
    protected final List<Consumer<StatsUpdateEvent>> listeners = new ArrayList<>();

    /**
     * Records token usage for a chat session.
     * 
     * @param sessionId unique session identifier
     * @param usage     the token usage statistics
     */
    public void recordUsage(String sessionId, TokenUsageStats usage) {
        sessionUsage.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(usage);
        notifyListeners(new StatsUpdateEvent(StatsUpdateEvent.Type.USAGE, sessionId, usage));
    }

    /**
     * Records performance metrics for a chat response.
     * 
     * @param sessionId unique session identifier
     * @param metrics   the performance metrics
     */
    public void recordMetrics(String sessionId, ChatPerformanceMetrics metrics) {
        sessionMetrics.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(metrics);
        notifyListeners(new StatsUpdateEvent(StatsUpdateEvent.Type.METRICS, sessionId, metrics));
    }

    /**
     * Gets all token usage stats for a session.
     */
    public List<TokenUsageStats> getSessionUsage(String sessionId) {
        return new ArrayList<>(sessionUsage.getOrDefault(sessionId, new ArrayList<>()));
    }

    /**
     * Gets all performance metrics for a session.
     */
    public List<ChatPerformanceMetrics> getSessionMetrics(String sessionId) {
        return new ArrayList<>(sessionMetrics.getOrDefault(sessionId, new ArrayList<>()));
    }

    /**
     * Gets aggregated daily usage for all sessions.
     * 
     * @return map of date to total tokens used that day
     */
    public Map<LocalDate, DailyUsageSummary> getDailyUsage() {
        Map<LocalDate, DailyUsageSummary> daily = new HashMap<>();

        sessionUsage.values().forEach(usages -> {
            usages.forEach(usage -> {
                LocalDate date = usage.date();
                daily.computeIfAbsent(date, k -> new DailyUsageSummary(date))
                        .addUsage(usage);
            });
        });

        return daily;
    }

    /**
     * Gets aggregated monthly usage for all sessions.
     * 
     * @return map of year-month to total tokens used that month
     */
    public Map<String, MonthlyUsageSummary> getMonthlyUsage() {
        Map<String, MonthlyUsageSummary> monthly = new HashMap<>();

        sessionUsage.values().forEach(usages -> {
            usages.forEach(usage -> {
                String key = String.format("%d-%02d",
                        usage.timestamp().getYear(),
                        usage.timestamp().getMonthValue());
                monthly.computeIfAbsent(key, k -> new MonthlyUsageSummary(key))
                        .addUsage(usage);
            });
        });

        return monthly;
    }

    /**
     * Gets today's total token usage.
     */
    public DailyUsageSummary getTodayUsage() {
        return getDailyUsage().getOrDefault(LocalDate.now(), new DailyUsageSummary(LocalDate.now()));
    }

    /**
     * Gets this month's total token usage.
     */
    public MonthlyUsageSummary getCurrentMonthUsage() {
        String key = String.format("%d-%02d",
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue());
        return getMonthlyUsage().getOrDefault(key, new MonthlyUsageSummary(key));
    }

    /**
     * Gets average performance metrics across all sessions.
     */
    public AggregatedMetrics getAverageMetrics() {
        List<ChatPerformanceMetrics> allMetrics = new ArrayList<>();
        sessionMetrics.values().forEach(allMetrics::addAll);

        if (allMetrics.isEmpty()) {
            return AggregatedMetrics.empty();
        }

        double avgTtft = allMetrics.stream().mapToDouble(ChatPerformanceMetrics::ttftMs).average().orElse(0.0);
        double avgTokensPerSec = allMetrics.stream().mapToDouble(ChatPerformanceMetrics::tokensPerSecond).average()
                .orElse(0.0);
        double avgDuration = allMetrics.stream().mapToDouble(ChatPerformanceMetrics::durationMs).average().orElse(0.0);
        int totalGenerated = allMetrics.stream().mapToInt(ChatPerformanceMetrics::generatedTokens).sum();
        int totalPrompt = allMetrics.stream().mapToInt(ChatPerformanceMetrics::promptTokens).sum();

        return new AggregatedMetrics(
                allMetrics.size(),
                avgTtft,
                avgTokensPerSec,
                avgDuration,
                totalGenerated,
                totalPrompt);
    }

    /**
     * Registers a listener for stats update events.
     */
    public void addListener(Consumer<StatsUpdateEvent> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    public void removeListener(Consumer<StatsUpdateEvent> listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(StatsUpdateEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }

    /**
     * Clears all tracked statistics.
     */
    public void clear() {
        sessionUsage.clear();
        sessionMetrics.clear();
    }

    /**
     * Represents a summary of daily token usage.
     */
    public static class DailyUsageSummary {
        private final LocalDate date;
        private long totalTokens = 0;
        private long inputTokens = 0;
        private long outputTokens = 0;
        private double totalCost = 0.0;
        private int sessionCount = 0;

        public DailyUsageSummary(LocalDate date) {
            this.date = date;
        }

        public void addUsage(TokenUsageStats usage) {
            totalTokens += usage.totalTokens();
            inputTokens += usage.inputTokens();
            outputTokens += usage.outputTokens();
            totalCost += usage.cost();
            sessionCount++;
        }

        public LocalDate date() {
            return date;
        }

        public long totalTokens() {
            return totalTokens;
        }

        public long inputTokens() {
            return inputTokens;
        }

        public long outputTokens() {
            return outputTokens;
        }

        public double totalCost() {
            return totalCost;
        }

        public int sessionCount() {
            return sessionCount;
        }

        public double avgTokensPerSession() {
            return sessionCount > 0 ? (double) totalTokens / sessionCount : 0.0;
        }
    }

    /**
     * Represents a summary of monthly token usage.
     */
    public static class MonthlyUsageSummary {
        private final String yearMonth;
        private long totalTokens = 0;
        private long inputTokens = 0;
        private long outputTokens = 0;
        private double totalCost = 0.0;
        private int sessionCount = 0;
        private int dayCount = 0;

        public MonthlyUsageSummary(String yearMonth) {
            this.yearMonth = yearMonth;
        }

        public void addUsage(TokenUsageStats usage) {
            totalTokens += usage.totalTokens();
            inputTokens += usage.inputTokens();
            outputTokens += usage.outputTokens();
            totalCost += usage.cost();
            sessionCount++;
        }

        public String yearMonth() {
            return yearMonth;
        }

        public long totalTokens() {
            return totalTokens;
        }

        public long inputTokens() {
            return inputTokens;
        }

        public long outputTokens() {
            return outputTokens;
        }

        public double totalCost() {
            return totalCost;
        }

        public int sessionCount() {
            return sessionCount;
        }

        public int dayCount() {
            return dayCount;
        }

        public double avgTokensPerDay() {
            return dayCount > 0 ? (double) totalTokens / dayCount : 0.0;
        }
    }

    /**
     * Represents aggregated performance metrics.
     */
    public record AggregatedMetrics(
            int sampleCount,
            double avgTtftMs,
            double avgTokensPerSecond,
            double avgDurationMs,
            int totalGeneratedTokens,
            int totalPromptTokens) {
        public static AggregatedMetrics empty() {
            return new AggregatedMetrics(0, 0.0, 0.0, 0.0, 0, 0);
        }
    }

    /**
     * Event fired when statistics are updated.
     */
    public record StatsUpdateEvent(
            Type type,
            String sessionId,
            Object data) {
        public enum Type {
            USAGE,
            METRICS
        }

        @SuppressWarnings("unchecked")
        public <T> T getData() {
            return (T) data;
        }
    }
}
