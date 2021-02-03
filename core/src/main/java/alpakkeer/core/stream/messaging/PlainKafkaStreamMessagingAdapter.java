package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.*;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.config.KafkaMessagingAdapterConfiguration;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.RecordContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.FutureConverters.FutureOps;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The AlpakkeerKafkaCommitableRecordContext stores the original [ConsumerMessage.CommitableOffset] from kafka
 * and commits the message once commit is called.
 */
@AllArgsConstructor(staticName = "apply")
class AlpakkeerKafkaCommitableRecordContext implements CommittableRecordContext {

    ConsumerMessage.CommittableOffset committableOffset;

    @Override
    public CompletionStage<Done> commit() {
       return new FutureOps<Done>(committableOffset.commitInternal()).asJava();
    }
}

@AllArgsConstructor(staticName = "apply")
public final class PlainKafkaStreamMessagingAdapter implements StreamMessagingAdapter {

   private static final Logger LOG = LoggerFactory.getLogger(PlainKafkaStreamMessagingAdapter.class);

   private final ActorSystem system;

   private final ObjectMapper om;

   private final KafkaMessagingAdapterConfiguration configuration;

   @Override
   public String getDefaultGroupId() {
      return configuration.getConsumer().getString("group-id");
   }

   @Override
   public <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record) {
      return Source
         .single(record)
         .toMat(recordsSink(topic), Keep.right())
         .run(system);
   }

   @Override
   public <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic) {
      return this.<R, C>recordsFlow(topic).toMat(Sink.ignore(), Keep.right());
   }

   @Override
   public <R, C extends RecordContext> Flow<Record<R, C>, Record<R, C>, NotUsed> recordsFlow(String topic) {
      var settings = ProducerSettings
         .create(configuration.getProducer(), new StringSerializer(), new StringSerializer())
         .withBootstrapServers(configuration.getBootstrapServer());

      return Flow
         .<Record<R, C>>create()
         .map(record -> ProducerMessage
            .single(
               new ProducerRecord<>(topic, record.getKey(), om.writeValueAsString(record)),
               record))
         .via(Producer.flexiFlow(settings))
         .map(ProducerMessage.Results::passThrough)
         .map(record -> {
            if (record.getContext() instanceof CommittableRecordContext) {
               ((CommittableRecordContext) record.getContext()).commit();
            }

            return record;
         });
   }

   @Override
   public <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType, String consumerGroup) {
      LOG.warn("putNextRecord is not implemented for PlainKafkaStreamMessagingAdapter");
      return CompletableFuture.completedFuture(Optional.empty());
   }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType, String consumerGroup) {
        var consumerSettings = ConsumerSettings.create(configuration.getConsumer(), new StringDeserializer(), new StringDeserializer())
                .withBootstrapServers(configuration.getBootstrapServer())
                .withGroupId(consumerGroup)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var committerSettings = CommitterSettings.create(configuration.getCommitter());

        return Consumer
                .committableSource(consumerSettings, Subscriptions.topics(topic))
                .mapMaterializedValue(mat -> NotUsed.getInstance())
                .map(committableMessage -> (Record<T, CommittableRecordContext>) om
                        .readValue(committableMessage.record().value(), Record.class)
                        .withContext(AlpakkeerKafkaCommitableRecordContext.apply(
                                committableMessage.committableOffset()
                        ))
                );
    }
}
