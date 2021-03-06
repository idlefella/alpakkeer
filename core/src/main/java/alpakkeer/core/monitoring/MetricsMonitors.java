package alpakkeer.core.monitoring;

import alpakkeer.core.monitoring.values.Marker;
import alpakkeer.core.monitoring.values.TimeSeries;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class MetricsMonitors implements MetricsMonitor {

   private final List<MetricsMonitor> monitors;

   public static MetricsMonitors apply() {
      return MetricsMonitors.apply(Lists.newArrayList());
   }

   public MetricsMonitors withMonitor(MetricsMonitor monitor) {
      this.monitors.add(monitor);
      return this;
   }

   @Override
   public List<MetricStore<List<Marker>>> getMarkerMetrics() {
      return monitors
         .stream()
         .flatMap(m -> m.getMarkerMetrics().stream())
         .collect(Collectors.toList());
   }

   @Override
   public List<MetricStore<TimeSeries>> getTimeSeriesMetrics() {
      return monitors
         .stream()
         .flatMap(m -> m.getTimeSeriesMetrics().stream())
         .collect(Collectors.toList());
   }

}
