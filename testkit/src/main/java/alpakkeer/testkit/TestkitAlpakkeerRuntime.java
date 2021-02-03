package alpakkeer.testkit;

import akka.actor.ActorSystem;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.context.ContextStore;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import alpakkeer.javadsl.AlpakkeerRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.prometheus.client.CollectorRegistry;

import java.util.List;

public class TestkitAlpakkeerRuntime extends AlpakkeerRuntime {

    public TestkitAlpakkeerRuntime(Javalin app, AlpakkeerConfiguration alpakkeerConfiguration, ActorSystem system, ObjectMapper objectMapper, CollectorRegistry collectorRegistry, ContextStore contextStore, List<MetricsCollector> metricsCollectors, CronScheduler scheduler, StreamMessagingAdapter messaging) {
        super(
                app,
                alpakkeerConfiguration,
                system,
                objectMapper,
                collectorRegistry,
                contextStore, metricsCollectors, scheduler, messaging
        );
    }

    @Override
    public CollectorRegistry getCollectorRegistry() {
        return new CollectorRegistry();
    }
}
