package alpakkeer.core.jobs.actor.protocol;

import akka.actor.typed.ActorRef;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.model.CronExpression;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Scheduled<P, C> implements Message<P, C> {

   String name;

   CronExpression cron;

   P properties;

   boolean queue;

   ActorRef<ScheduledExecution<P>> replyTo;

}
