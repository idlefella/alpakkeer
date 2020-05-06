package alpakkeer.core.resources;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.Job;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.Jobs;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.values.Name;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Resources {

   private final ActorSystem system;

   private final CronScheduler scheduler;

   private final AtomicReference<Map<Name, Job<?>>> jobs;

   public static Resources apply(ActorSystem system, CronScheduler scheduler) {
      return new Resources(system, scheduler, new AtomicReference<>(Maps.newHashMap()));
   }

   public <P> Job<P> addJob(JobDefinition<P> jobDefinition) {
      AtomicReference<Job<P>> result = new AtomicReference<>();

      jobs.getAndUpdate(currentJobs -> {
         if (currentJobs.containsKey(jobDefinition.getName())) {
            throw JobAlreadyExistsException.apply(jobDefinition.getName());
         } else {
            var job = Jobs.apply(system, scheduler, jobDefinition);
            currentJobs.put(jobDefinition.getName(), job);
            result.set(job);
            return currentJobs;
         }
      });

      return result.get();
   }

   public List<Job<?>> getJobs() {
      return jobs.get()
         .values()
         .stream()
         .sorted(Comparator.comparing(j -> j.getDefinition().getName().getValue()))
         .collect(Collectors.toList());
   }

   public Optional<Job<?>> getJob(Name name) {
      return Optional.ofNullable(jobs.get().get(name));
   }

}