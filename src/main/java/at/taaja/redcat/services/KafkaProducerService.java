package at.taaja.redcat.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.JacksonSerializer;
import io.taaja.kafka.Topics;
import io.taaja.models.message.KafkaMessage;
import io.taaja.models.record.spatial.SpatialEntity;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

@ApplicationScoped
@JBossLog
public class KafkaProducerService {

    @ConfigProperty(name = "kafka.bootstrap-servers")
    private String bootstrapServers;

    private Producer<String, KafkaMessage> kafkaProducer;
    private ObjectMapper objectMapper;

    public static String originatorId;


    void onStart(@Observes StartupEvent ev) {
        if(KafkaProducerService.originatorId == null){
            KafkaProducerService.originatorId = UUID.randomUUID().toString();
        }
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, this.originatorId);
        this.objectMapper = new ObjectMapper();
        this.kafkaProducer = new KafkaProducer(producerProperties, new StringSerializer(), new JacksonSerializer());
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        log.info("shutdown kafka");
        this.kafkaProducer.close();
    }

    public void publish(final KafkaMessage kafkaMessage, final Iterable<SpatialEntity> targets){
        targets.forEach(spatialEntity -> this.publish(kafkaMessage, spatialEntity.getId()));
    }

    public void publish(final KafkaMessage kafkaMessage, final String targetId){

        kafkaMessage.setPublisherId(this.originatorId);

        KafkaProducerService.this.kafkaProducer.send(
                new ProducerRecord<>(
                        Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX + targetId,
                        KafkaProducerService.originatorId + "/" + UUID.randomUUID().toString(),
                        kafkaMessage
                )
        );

    }

}
