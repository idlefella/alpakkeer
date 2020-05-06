package alpakkeer.core.jobs.monitor;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface JobMonitor<P> {

   void onTriggered(String executionId, P properties);

   void onStarted(String executionId);

   void onFailed(String executionId, Throwable cause);

   void onCompleted(String executionId);

   void onStopped(String executionId);

   void onQueued(int newQueueSize);

   void onEnqueued(int newQueueSize);

   CompletionStage<Optional<Object>> getStatus();

}