package alpakkeer.testkit;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.jobs.JobDefinitions;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;


public class AlpakkeerTestkitTest {

    private JobDefinitions.JobSettingsConfiguration<String, List<String>> getTestJob(JobDefinitions jobs) {
        return jobs
                .create("testjob", "defaultProperty", List.of("test"))
                .runGraph(jb -> Source.single(jb.getProperties()).via(jb.monitoring().createCheckpointMonitor("testCheckpointMonitor", Duration.of(1, ChronoUnit.SECONDS))).toMat(Sink.seq(), Keep.right()));
    }

    @Test
    public void runJobTest() throws ExecutionException, InterruptedException, TimeoutException {
        var result = AlpakkeerTestkit.runJob(this::getTestJob, "hello", List.of("test"));
        var x = result.toCompletableFuture().get(60, TimeUnit.SECONDS);
        var y = x.getCompletion().toCompletableFuture().get();
        assertEquals(y.get(), List.of("hello"));
    }
}
