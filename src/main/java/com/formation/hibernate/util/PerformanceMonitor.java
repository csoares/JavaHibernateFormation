package com.formation.hibernate.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

    private final Map<String, Instant> startTimes = new ConcurrentHashMap<>();
    private final Map<String, PerformanceResult> results = new ConcurrentHashMap<>();

    public static class PerformanceResult {
        private final String operation;
        private final long durationMs;
        private final Instant timestamp;
        private final String description;

        public PerformanceResult(String operation, long durationMs, Instant timestamp, String description) {
            this.operation = operation;
            this.durationMs = durationMs;
            this.timestamp = timestamp;
            this.description = description;
        }

        public String getOperation() { return operation; }
        public long getDurationMs() { return durationMs; }
        public Instant getTimestamp() { return timestamp; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %d ms - %s",
                timestamp, operation, durationMs, description);
        }
    }

    public void start(String operationId) {
        start(operationId, "");
    }

    public void start(String operationId, String description) {
        startTimes.put(operationId, Instant.now());
        logger.info("🚀 INICIANDO: {} - {}", operationId, description);
    }

    public PerformanceResult stop(String operationId) {
        return stop(operationId, "");
    }

    public PerformanceResult stop(String operationId, String description) {
        Instant endTime = Instant.now();
        Instant startTime = startTimes.remove(operationId);

        if (startTime == null) {
            logger.warn("⚠️  Operação '{}' não foi iniciada", operationId);
            return null;
        }

        long durationMs = Duration.between(startTime, endTime).toMillis();
        PerformanceResult result = new PerformanceResult(operationId, durationMs, endTime, description);

        results.put(operationId, result);

        String emoji = getPerformanceEmoji(durationMs);
        logger.info("{} FINALIZADO: {} - {} ms - {}",
            emoji, operationId, durationMs, description);

        return result;
    }

    public <T> T measure(String operationId, String description, java.util.function.Supplier<T> operation) {
        start(operationId, description);
        try {
            T result = operation.get();
            stop(operationId, description);
            return result;
        } catch (Exception e) {
            stop(operationId, "ERRO: " + e.getMessage());
            throw e;
        }
    }

    public void measure(String operationId, String description, Runnable operation) {
        start(operationId, description);
        try {
            operation.run();
            stop(operationId, description);
        } catch (Exception e) {
            stop(operationId, "ERRO: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, PerformanceResult> getAllResults() {
        return new ConcurrentHashMap<>(results);
    }

    public void clearResults() {
        results.clear();
        startTimes.clear();
    }

    public void printSummary() {
        logger.info("📊 === RESUMO DE PERFORMANCE ===");
        results.values().stream()
            .sorted((a, b) -> Long.compare(b.getDurationMs(), a.getDurationMs()))
            .forEach(result -> logger.info("   {}", result));
        logger.info("📊 === FIM DO RESUMO ===");
    }

    private String getPerformanceEmoji(long durationMs) {
        if (durationMs < 50) return "⚡";
        if (durationMs < 200) return "✅";
        if (durationMs < 1000) return "⚠️";
        return "🐌";
    }

    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + " ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2f s", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%d min %d s", minutes, seconds);
        }
    }
}