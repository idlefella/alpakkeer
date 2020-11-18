package alpakkeer.core.processes.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import alpakkeer.core.util.Strings;
import com.google.common.collect.Maps;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
final public class PrometheusProcessMonitor implements ProcessMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusProcessMonitor.class);

    public static PrometheusProcessMonitor apply(String name, CollectorRegistry registry) {
        var nameSC = Strings.convert(name).toSnakeCase();

        Gauge processDuration = Gauge
                .build()
                .name(String.format("%s__process_duration_seconds", nameSC))
                .help(String.format("The duration of the process `%s` in seconds", nameSC))
                .create()
                .register(registry);

        Summary processDurations = Summary
                .build()
                .name(String.format("%s__process_durations_seconds", nameSC))
                .help(String.format("A summary of recorded durations of process `%s` in seconds", nameSC))
                .create()
                .register(registry);

        return apply(
                nameSC, processDuration, processDurations, Maps.newConcurrentMap(),
                Maps.newConcurrentMap(), Maps.newConcurrentMap(), Maps.newConcurrentMap(),
                Maps.newConcurrentMap(), Maps.newConcurrentMap(), Maps.newConcurrentMap(),
                registry);
    }

    @Value
    @AllArgsConstructor(staticName = "apply")
    private static class RunningJob {
        long startNanos;
    }

    private final String name;

    private final Gauge processDuration;

    private final Summary processDurations;

    private final Map<String, PrometheusProcessMonitor.RunningJob> runningExecutions;

    private final Map<String, Gauge> pushPullLatencies;

    private final Map<String, Gauge> pullPushLatencies;

    private final Map<String, Counter> counts;

    private final Map<String, Gauge> countsInterval;

    private final Map<String, Gauge> throughputs;

    private final Map<String, Gauge> latencies;

    private final CollectorRegistry registry;

    @Override
    public void onStarted(String executionId) {
        // Do nothing
    }

    @Override
    public void onFailed(String executionId, Throwable cause, Instant nextRetry) {
        runningExecutions.remove(executionId);
    }

    @Override
    public void onCompletion(String executionId, Instant nextStart) {
        if (runningExecutions.containsKey(executionId)) {
            var exec = runningExecutions.remove(executionId);
            var seconds = Duration.ofNanos(System.nanoTime() - exec.startNanos).getSeconds();
            processDurations.observe(seconds);
            processDuration.set(seconds);
        }
        finish();
    }

    private void finish() {
        counts.values().forEach(Counter::clear);
        countsInterval.values().forEach(Gauge::clear);
        pushPullLatencies.values().forEach(Gauge::clear);
        pullPushLatencies.values().forEach(Gauge::clear);
        throughputs.values().forEach(Gauge::clear);
        latencies.values().forEach(Gauge::clear);
    }

    @Override
    public void onStopped(String executionId) {
        runningExecutions.remove(executionId);
        finish();
    }

    @Override
    public CompletionStage<Optional<Object>> getStatus() {
        return null;
    }

    @Override
    public void onStats(String executionId, String name, CheckpointMonitor.Stats statistics) {
        var nameSC = Strings.convert(name).toSnakeCase();

        try {
            if (!counts.containsKey(nameSC)) {
                counts.put(nameSC, Counter
                        .build(
                                String.format("%s__%s__count_sum", this.name, nameSC),
                                "Number of documents processed by this checkpoint")
                        .register(registry));
            }

            if (!countsInterval.containsKey(nameSC)) {
                countsInterval.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__count_current", this.name, nameSC),
                                "Number of documents processed by this checkpoint within measurement interval")
                        .register(registry));
            }

            if (!throughputs.containsKey(nameSC)) {
                throughputs.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__throughout_per_second", this.name, nameSC),
                                "Throughput processed by this checkpoint within measurement interval")
                        .register(registry));
            }

            if (!pullPushLatencies.containsKey(nameSC)) {
                pullPushLatencies.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__pull_push_latency_in_ns", this.name, nameSC),
                                "Average pull-push latency within last interval")
                        .register(registry));
            }

            if (!pushPullLatencies.containsKey(nameSC)) {
                pushPullLatencies.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__push_pull_latency_in_ns", this.name, nameSC),
                                "Average pull-push latency within last interval")
                        .register(registry));
            }

            counts.get(nameSC).inc(statistics.count());
            countsInterval.get(nameSC).set(statistics.count());
            throughputs.get(nameSC).set(statistics.throughputElementsPerSecond());
            pullPushLatencies.get(nameSC).set(statistics.pullPushLatencyNanos());
            pushPullLatencies.get(nameSC).set(statistics.pushPullLatencyNanos());
        } catch (Exception ex) {
            logger.warn("An exception occurred while updating Prometheus metrics", ex);
        }
    }

    @Override
    public void onStats(String executionId, String name, LatencyMonitor.Stats statistics) {
        var nameSC = Strings.convert(name).toSnakeCase();

        try {
            if (!counts.containsKey(nameSC)) {
                counts.put(nameSC, Counter
                        .build(
                                String.format("%s__%s__count_sum", this.name, nameSC),
                                "Number of documents processed by this checkpoint")
                        .register(registry));
            }

            if (!countsInterval.containsKey(nameSC)) {
                countsInterval.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__count_current", this.name, nameSC),
                                "Number of documents processed by this checkpoint within measurement interval")
                        .register(registry));
            }

            if (!latencies.containsKey(nameSC)) {
                latencies.put(nameSC, Gauge
                        .build(
                                String.format("%s__%s__latency_in_ms", this.name, nameSC),
                                "Average latency of stage within last interval in milliseconds.")
                        .register(registry));
            }

            counts.get(nameSC).inc(statistics.count());
            countsInterval.get(nameSC).set(statistics.count());
            latencies.get(nameSC).set(statistics.avgLatency());
        } catch (Exception ex) {
            logger.warn("An exception occurred while updating Prometheus metrics", ex);
        }
    }
}
