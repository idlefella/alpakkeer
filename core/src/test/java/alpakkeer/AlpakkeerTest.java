package alpakkeer;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.javadsl.Alpakkeer;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AlpakkeerTest {

   @Test
   public void simpleTest() throws ExecutionException, InterruptedException {
      var alpakkeer = Alpakkeer
         .create()
         .withJob(
            jobs -> jobs
               .create("hello-world")
               .runGraph(sb -> Source
                  .single("Hello World")
                  .toMat(Sink.foreach(System.out::println), Keep.right()))
               .withLoggingMonitor())
         .start();

      alpakkeer
         .getResources()
         .getJob("hello-world")
         .start()
         .toCompletableFuture()
         .get();

      alpakkeer.stop().toCompletableFuture().get();
   }

   @Test
   public void messagingTest() throws InterruptedException, ExecutionException {
      var list = Lists.<String>newArrayList();
      var alpakkeer = Alpakkeer
         .create()
         .withJob(
            jobs -> jobs
               .create("hello-world")
               .runGraph(sb -> Source
                  .from(Lists.newArrayList("Hallo", "Welt"))
                       .via(sb.monitoring().createCheckpointMonitor("testMonitor", Duration.ofSeconds(1)))
                  .toMat(sb.messaging().itemsSink("test"), Keep.right()))
               .withLoggingMonitor()
                    .withPrometheusMonitor())
         .withProcess(p -> p
            .create("process")
            .runGraph(sb -> sb
               .messaging()
               .recordsSource("test", String.class)
               .map(record -> {
                  list.add(record.getValue());
                  record.getContext().commit();
                  return record;
               })
               .toMat(Sink.ignore(), Keep.right()))
            .withLoggingMonitor().withPrometheusMonitor())
         .start();

      alpakkeer
         .getResources()
         .getJob("hello-world")
         .start();

      Thread.sleep(5000);
      assertTrue(list.contains("Hallo"));
      assertTrue(list.contains("Welt"));
      assertEquals(2, list.size());

      alpakkeer.stop().toCompletableFuture().get();
   }

   @Test
   public void checkpointMonitorTest() throws InterruptedException, ExecutionException {
      var alpakkeer = Alpakkeer
              .create()
              .withJob(
                      jobs -> jobs
                              .create("checkpoint-monitor-test")
                              .runGraph(sb -> Source.from(Lists.newArrayList("test1", "test2", "test3"))
                                      .via(sb.monitoring().createCheckpointMonitor("testMonitor", Duration.ofSeconds(2)))
                                      .toMat(Sink.foreach(System.out::println), Keep.right()))
                              .withLoggingMonitor())
              .start();

      alpakkeer
              .getResources()
              .getJob("checkpoint-monitor-test")
              .start()
              .toCompletableFuture()
              .get();

      alpakkeer.stop().toCompletableFuture().get();
   }

}
