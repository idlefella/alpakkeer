package alpakkeer.testkit;

import akka.actor.ActorSystem;
import akka.japi.function.Function;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.context.InMemoryContextStore;
import alpakkeer.core.stream.messaging.InMemoryStreamMessagingAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import alpakkeer.core.util.ObjectMapperFactory;
import alpakkeer.core.util.Operators;
import alpakkeer.javadsl.AlpakkeerRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public class AlpakkeerTestkit {

    static private AlpakkeerConfiguration alpkakkeerConfiguration = AlpakkeerConfiguration.apply();

    static private ObjectMapper objectMapper = ObjectMapperFactory.apply().create(alpkakkeerConfiguration.getObjectMapperConfiguration());

    static private ActorSystem actorSystem = ActorSystem.apply("unittest");

    static private AlpakkeerRuntime alpakkeerRuntime = AlpakkeerRuntime.apply(
            null,
            alpkakkeerConfiguration,
            actorSystem,
            objectMapper,
            CollectorRegistry.defaultRegistry,
            InMemoryContextStore.apply(objectMapper),
            Collections.emptyList(),
            null,
            InMemoryStreamMessagingAdapter.apply(objectMapper)
    );

    private StreamMessagingAdapter messaging;

    static private JobDefinitions jobDefinitions = JobDefinitions.apply(alpakkeerRuntime);

    static public <P, C> CompletionStage<JobHandle<C>> runJob(Function<JobDefinitions, JobDefinitions.JobSettingsConfiguration<P, C>> builder, P properties, C context) {
        JobDefinition<P, C> job = Operators.suppressExceptions(() -> builder.apply(jobDefinitions).build());
        return job.run("unittest", properties, context);
    }
}
