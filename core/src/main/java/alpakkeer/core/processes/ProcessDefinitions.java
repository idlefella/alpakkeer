package alpakkeer.core.processes;

import akka.Done;
import akka.japi.Pair;
import akka.japi.function.Function;
import akka.japi.function.Procedure2;
import akka.stream.UniqueKillSwitch;
import akka.stream.javadsl.RunnableGraph;
import alpakkeer.config.ProcessConfiguration;
import alpakkeer.core.jobs.monitor.LoggingJobMonitor;
import alpakkeer.core.processes.monitor.LoggingProcessMonitor;
import alpakkeer.core.processes.monitor.ProcessMonitor;
import alpakkeer.core.processes.monitor.ProcessMonitorGroup;
import alpakkeer.core.processes.monitor.PrometheusProcessMonitor;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Strings;
import alpakkeer.javadsl.Alpakkeer;
import alpakkeer.javadsl.AlpakkeerRuntime;
import com.google.common.collect.Lists;
import io.javalin.Javalin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Factory for {@link ProcessDefinitions}.
 */
@AllArgsConstructor(staticName = "apply")
public final class ProcessDefinitions {

   private static final Logger LOG = LoggerFactory.getLogger(Alpakkeer.class);

   private final AlpakkeerRuntime runtimeConfiguration;

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PROTECTED)
   public static class ProcessRunnableBuilder {

      private final String name;

      private final AlpakkeerRuntime runtime;

      /**
       * Create a process execution factory which returns a completion stage of {@link java.lang.ProcessHandle}. The
       * process handle can be used to stop the process.
       *
       * @param run The factory method
       * @return A {@link ProcessDefinitionBuilder} instance
       */
      public ProcessDefinitionBuilder runCancellableCS(Function<ProcessStreamBuilder, CompletionStage<ProcessHandle>> run) {
         return ProcessDefinitionBuilder.apply(name, run, runtime);
      }

      /**
       * Create a process execution factory which returns a {@link java.lang.ProcessHandle}. The
       * process handle can be used to stop the process.
       *
       * @param run The factory method
       * @return A {@link ProcessDefinitionBuilder} instance
       */
      public ProcessDefinitionBuilder runCancellable(Function<ProcessStreamBuilder, ProcessHandle> run) {
         return runCancellableCS(s -> CompletableFuture.completedFuture(Operators.suppressExceptions(() -> run.apply(s))));
      }

      /**
       * Create a process execution factory which returns a completion stage. The process cannot be cancelled upon
       * request. Use @link{this#runCancellable(Function)} to create a process handle if process should be
       * cancellable.
       *
       * @param run The factory method
       * @return A {@link ProcessDefinitionBuilder} instance
       */
      public ProcessDefinitionBuilder runCS(Function<ProcessStreamBuilder, CompletionStage<?>> run) {
         return runCancellable(s -> ProcessHandles.createFromCS(run.apply(s)));
      }

      /**
       * Creates a process execution factory which returns a runnable Akka Streams graph. The Graph will be run to
       * execute the stream. The Process cannot be cancelled.
       *
       * @param graph The factory method
       * @return A {@link ProcessDefinitionBuilder} instance
       */
      public ProcessDefinitionBuilder runGraph(Function<ProcessStreamBuilder, RunnableGraph<CompletionStage<Done>>> graph) {
         return runCS(s -> graph.apply(s).run(runtime.getSystem()));
      }

      /**
       * Creates a process execution factory which returns a runnable Akka Streams graph which can be cancelled.
       *
       * @param graph The factory method
       * @return A {@link ProcessDefinitionBuilder} instance
       */
      public ProcessDefinitionBuilder runCancellableGraph(Function<ProcessStreamBuilder, RunnableGraph<Pair<UniqueKillSwitch, CompletionStage<Done>>>> graph) {
         return runCancellable(s -> ProcessHandles.createFromCancellableGraph(graph.apply(s).run(runtime.getSystem())));
      }

   }

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PROTECTED)
   public static class ProcessDefinitionBuilder {

      private final String name;

      private final Function<ProcessStreamBuilder, CompletionStage<ProcessHandle>> runner;

      private final AlpakkeerRuntime runtime;

      private Duration initialRetryBackoff;

      private Duration completionRestartBackoff;

      private Duration retryBackoffResetTimeout;

      private Logger logger;

      private boolean initiallyStarted;

      private boolean enabled;

      private final ProcessMonitorGroup monitors;

      private List<Procedure2<Javalin, Process>> apiExtensions;

      static ProcessDefinitionBuilder apply(
         String name, Function<ProcessStreamBuilder, CompletionStage<ProcessHandle>> runner, AlpakkeerRuntime runtime) {
         var logger = LoggerFactory.getLogger(String.format(
            "alpakkeer.processes.%s",
            Strings.convert(name).toSnakeCase()));

         return apply(
            name, runner, runtime, Duration.ofSeconds(10), Duration.ofMinutes(10),
            Duration.ofMinutes(1), logger, true, true, ProcessMonitorGroup.apply(),
            Lists.newArrayList());
      }

      /**
       * Finally creates the {@link ProcessDefinition}.
       *
       * @return The definition
       */
      public ProcessDefinition build() {
         return SimpleProcessDefinition.apply(
            name, initialRetryBackoff, completionRestartBackoff, retryBackoffResetTimeout, runner,
            logger, enabled, initiallyStarted, monitors, apiExtensions, runtime);
      }

      /**
       * The job definition can be disabled. If the definition is disabled, the job will not be started/ initialized
       * during startup of Alpakkeer.
       *
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder disabled() {
         this.enabled = false;
         return this;
      }

      /**
       * Set whether the process should be enabled or not. By default it is enabled. If the definition is disabled,
       * the process will not be started/ initialized during startup of Alpakkeer.
       *
       * @param enabled Whether the job is enabled or not.
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder enabled(boolean enabled) {
         this.enabled = enabled;
         return this;
      }

      /**
       * Enable the process. By default it is enabled. If the definition is disabled,
       * the process will not be started/ initialized during startup of Alpakkeer.
       *
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder enabled() {
         return enabled(true);
      }

      /**
       * Configure the process to be started upon application startup.
       *
       * @return This builder instance
       */
      public ProcessDefinitionBuilder initializeStarted() {
         this.initiallyStarted = true;
         return this;
      }

      /**
       * Configure the process to be started upon application startup.
       *
       * @param initializeStarted Whether the process should start on initialization of the app
       * @return This builder instance
       */
      public ProcessDefinitionBuilder initializeStarted(boolean initializeStarted) {
         this.initiallyStarted = initializeStarted;
         return this;
      }

      /**
       * Configure the process to stay in stopped state upon application startup.
       *
       * @return This builder instance
       */
      public ProcessDefinitionBuilder initializeStopped() {
         this.initiallyStarted = false;
         return this;
      }

      /**
       * Specify an additional API endpoint for the process. The procedure passed to this method may use the {@link Javalin}
       * instance to define API endpoints which also use/ access the related {@link Process} instance.
       *
       * @param apiExtension A procedure which can extend the {@link Javalin} API.
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withApiEndpoint(Procedure2<Javalin, Process> apiExtension) {
         apiExtensions.add(apiExtension);
         return this;
      }

      /**
       * Read configurations for the process from the provided config object.
       *
       * @param configuration The configuration object
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withConfiguration(ProcessConfiguration configuration) {
         if (configuration.isClearMonitors()) {
            this.monitors.clearMonitors();
         }

         var next = this
            .enabled(configuration.isEnabled())
            .initializeStarted(configuration.isInitializeStarted());

         for (String m : configuration.getMonitors()) {
            // TODO: Add more monitor types for processes
            switch (m) {
               case "logging":
                  next = next.withLoggingMonitor();
                  break;
               case "prometheus":
                  next = next.withPrometheusMonitor();
                  break;
               default:
                  LOG.warn("Unknown monitor type `{}` configured for process `{}`", m, name);
            }
         }

         return next;
      }

      /**
       * Use this method to enable configuration overrides (e.g. for different environments, etc.); The configuration
       * will be taken from default location `alpakkeer.processes` (which is a list of job configurations)
       *
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withConfiguration() {
         var config = runtime.getConfiguration().getProcessConfiguration(name);

         if (config.isPresent()) {
            return withConfiguration(config.get());
         } else {
            LOG.warn("No configuration object found for process `{}` within `alpakkeer.processes`", name);
            return this;
         }
      }

      /**
       * Configure the completion restart backoff. When the process is in running state and the underlying stream finishes,
       * the stream will be restarted. The restart backoff is the pause between the successful completion and the restart
       * of the underlying stream (the process execution factory will be called again).
       *
       * @param completionRestartBackoff The duration for the restart backoff
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withCompletionRestartBackoff(Duration completionRestartBackoff) {
         this.completionRestartBackoff = completionRestartBackoff;
         return this;
      }

      /**
       * The initial retry backoff is the pause between retries when the underlying stream fails. The restart backoff will
       * be doubled when the stream is failing again directly after restart.
       *
       * @param initialRetryBackoff The duration of the initial restart backoff
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withInitialRetryBackoff(Duration initialRetryBackoff) {
         this.initialRetryBackoff = initialRetryBackoff;
         return this;
      }

      /**
       * Enable an additional monitor for the process.
       *
       * @param monitor The monitor to be enabled.
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withMonitor(ProcessMonitor monitor) {
         this.monitors.withMonitor(monitor);
         return this;
      }

      /**
       * Enables a {@link LoggingJobMonitor} for the process. The monitor will log information and errors about the executions of the
       * process to the default logging infrastructure.
       *
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withLoggingMonitor() {
         return withMonitor(LoggingProcessMonitor.apply(name, logger));
      }

      /**
       * Enables a {@link PrometheusProcessMonitor} for the process. The monitor will log information about the executions of the process
       * in Prometheus Metrics.
       *
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withPrometheusMonitor() {
         return withMonitor(PrometheusProcessMonitor.apply(name, runtime.getCollectorRegistry()));
      }

      /**
       * The retry backoff reset timeout defines how long the process must run without a failure, that the retry backoff
       * gets reseted to its initial retry backoff.
       *
       * @param retryBackoffResetTimeout The duration of the timeout
       * @return The current instance of the builder
       */
      public ProcessDefinitionBuilder withRetryBackoffResetTimeout(Duration retryBackoffResetTimeout) {
         this.retryBackoffResetTimeout = retryBackoffResetTimeout;
         return this;
      }

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleProcessDefinition implements ProcessDefinition {

      private final String name;

      private final Duration initialBackoff;

      private final Duration completionRestartBackoff;

      private final Duration retryBackoffResetTimeout;

      private final Function<ProcessStreamBuilder, CompletionStage<ProcessHandle>> runner;

      private final Logger logger;

      private final boolean enabled;

      private final boolean initiallyStarted;

      private final ProcessMonitorGroup monitors;

      private final List<Procedure2<Javalin, Process>> apiExtensions;

      private final AlpakkeerRuntime runtime;

      @Override
      public void extendApi(Javalin api, Process processInstance) {
         apiExtensions.forEach(ext -> Operators.suppressExceptions(() -> ext.apply(api, processInstance)));
      }

      @Override
      public boolean isEnabled() {
         return enabled;
      }

      @Override
      public boolean isInitiallyStarted() {
         return initiallyStarted;
      }

      @Override
      public String getName() {
         return name;
      }

      @Override
      public Logger getLogger() {
         return logger;
      }

      @Override
      public ProcessMonitorGroup getMonitors() {
         return monitors;
      }

      @Override
      public Duration getInitialRetryBackoff() {
         return initialBackoff;
      }

      @Override
      public Duration getCompletionRestartBackoff() {
         return completionRestartBackoff;
      }

      @Override
      public Duration getRetryBackoffResetTimeout() {
         return retryBackoffResetTimeout;
      }

      @Override
      public CompletionStage<ProcessHandle> run(String executionId) {
         return Operators.suppressExceptions(() -> runner.apply(ProcessStreamBuilder.apply(runtime, getMonitors(), executionId, logger, name)));
      }

   }

   /**
    * Start creating a new process definition.
    *
    * @param name The name of the process.
    * @return A new builder instance
    */
   public ProcessRunnableBuilder create(String name) {
      return ProcessRunnableBuilder.apply(name, runtimeConfiguration);
   }

   /**
    * Access other Alpakkeer runtime components during job definition.
    *
    * @return The initialized Alpakkeer runtime
    */
   public AlpakkeerRuntime getRuntime() {
      return runtimeConfiguration;
   }

}
