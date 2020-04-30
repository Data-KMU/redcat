package at.taaja.redcat;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.JacksonSerializer;
import io.taaja.kafka.Topics;
import io.taaja.models.message.extension.operation.SpatialOperation;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

@ApplicationScoped
@JBossLog
public class KafkaProducerService {

    @ConfigProperty(name = "kafka.bootstrap-servers")
    private String bootstrapServers;

    private Producer<Long, SpatialOperation> kafkaProducer;


    void onStart(@Observes StartupEvent ev) {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());

        this.kafkaProducer = new KafkaProducer(producerProperties, new LongSerializer(), new JacksonSerializer());
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        log.info("shutdown kafka");
        this.kafkaProducer.close();
    }


    public void publish(SpatialOperation spatialOperation) {

        //todo: populate spatialOperation.getIntersectingExtensions() and publish (ASYNC)
        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("c56b3543-6853-4d86-a7bc-1cde673a5582");
        spatialOperation.setIntersectingExtensions(stringArrayList);

        for(String extensionId : spatialOperation.getIntersectingExtensions()){
            this.kafkaProducer.send(
                new ProducerRecord<>(
                    Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX + extensionId,
                    spatialOperation
                )
            );
        }
    }
}
