package alpakkeer.testkit;

import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.Materializer;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.context.InMemoryContextStore;
import alpakkeer.core.processes.ProcessDefinitions;
import alpakkeer.core.processes.ProcessHandle;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapters;
import alpakkeer.core.util.ObjectMapperFactory;
import alpakkeer.core.util.Operators;
import alpakkeer.javadsl.AlpakkeerBaseRuntime;
import alpakkeer.javadsl.AlpakkeerRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@AllArgsConstructor(staticName = "apply")
public class AlpakkeerTestkit {

    static private AlpakkeerConfiguration alpkakkeerConfiguration = AlpakkeerConfiguration.apply();

    static public ObjectMapper objectMapper = ObjectMapperFactory.apply().create(alpkakkeerConfiguration.getObjectMapperConfiguration());

    static public ActorSystem actorSystem = ActorSystem.apply("unittest");

    static public Materializer materializer = Materializer.matFromSystem(actorSystem);

    static private AlpakkeerBaseRuntime baseRuntime = AlpakkeerBaseRuntime.apply(null, alpkakkeerConfiguration, actorSystem, objectMapper);

    static public StreamMessagingAdapter streamMessagingAdapter = StreamMessagingAdapters.createFromConfiguration(baseRuntime);

    static public AlpakkeerRuntime alpakkeerRuntime = new TestkitAlpakkeerRuntime(
            null,
            alpkakkeerConfiguration,
            actorSystem,
            objectMapper,
            null,
            InMemoryContextStore.apply(objectMapper),
            Collections.emptyList(),
            null,
            streamMessagingAdapter
    );

    static private JobDefinitions jobDefinitions = JobDefinitions.apply(alpakkeerRuntime);

    static private ProcessDefinitions processDefinitions = ProcessDefinitions.apply(alpakkeerRuntime);

    static public <P, C> CompletionStage<JobHandle<C>> runJob(Function<JobDefinitions, JobDefinitions.JobSettingsConfiguration<P, C>> builder, P properties, C context) {
        JobDefinition<P, C> job = Operators.suppressExceptions(() -> builder.apply(jobDefinitions).build());
        return job.run("unittest", properties, context);
    }

    static public <C> Optional<C> waitForJob(CompletionStage<JobHandle<C>> jobHandle, long timeoutInSeconds) throws InterruptedException, ExecutionException, TimeoutException {
        return jobHandle.toCompletableFuture().get(timeoutInSeconds, TimeUnit.SECONDS).getCompletion().toCompletableFuture().get(timeoutInSeconds, TimeUnit.SECONDS);
    }

    static public <C> Optional<C> waitForJob(CompletionStage<JobHandle<C>> jobHandle) throws InterruptedException, ExecutionException, TimeoutException {
        return waitForJob(jobHandle, 60);
    }

    static public <P, C> Optional<C> runAndWaitForJob(Function<JobDefinitions, JobDefinitions.JobSettingsConfiguration<P, C>> builder, P properties, C context) throws InterruptedException, ExecutionException, TimeoutException {
        return waitForJob(runJob(builder, properties, context));
    }

    static public ProcessHandle runProcess(Function<ProcessDefinitions, ProcessDefinitions.ProcessDefinitionBuilder> builder) throws Exception {
        var processDefinition = builder.apply(processDefinitions).build();
        return processDefinition.run("unittest").toCompletableFuture().get();
    }
}
