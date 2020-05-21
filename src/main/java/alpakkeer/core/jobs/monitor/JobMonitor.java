package alpakkeer.core.jobs.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface JobMonitor<P, C> {

   void onTriggered(String executionId, P properties);

   void onStarted(String executionId);

   void onFailed(String executionId, Throwable cause);

   void onCompleted(String executionId, C result);

   void onCompleted(String executionId);

   void onStats(String executionId, String name, CheckpointMonitor.Stats statistics);

   void onStats(String executionId, String name, LatencyMonitor.Stats statistics);

   void onStopped(String executionId, C result);

   void onStopped(String executionId);

   void onQueued(int newQueueSize);

   void onEnqueued(int newQueueSize);

   CompletionStage<Optional<Object>> getStatus();

}
