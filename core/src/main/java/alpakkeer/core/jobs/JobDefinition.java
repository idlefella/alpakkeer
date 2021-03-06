package alpakkeer.core.jobs;

import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.jobs.monitor.JobMonitorGroup;
import io.javalin.Javalin;
import org.slf4j.Logger;

import java.util.List;

public interface JobDefinition<P, C> extends JobRunner<P, C> {

   void extendApi(Javalin api, Job<P, C> jobInstance);

   P getDefaultProperties();

   C getInitialContext();

   boolean isEnabled();

   String getName();

   Logger getLogger();

   List<ScheduleExecution<P>> getSchedule();

   JobMonitorGroup<P, C> getMonitors();

}
