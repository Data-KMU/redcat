package at.taaja.redcat;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.JacksonSerializer;
import io.taaja.kafka.Topics;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.record.spatial.Area;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.models.views.SpatialRecordView;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
@JBossLog
public class KafkaProducerService {

    @Inject
    IntersectingExtensionsService intersectingExtensionsService;

    @Inject
    IdTrackerService idTrackerService;

    @ConfigProperty(name = "kafka.bootstrap-servers")
    private String bootstrapServers;

    private Producer<String, SpatialOperation> kafkaProducer;
    private ExecutorService publishExecutor;
    private ObjectMapper objectMapper;


    void onStart(@Observes StartupEvent ev) {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        this.publishExecutor = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
        this.kafkaProducer = new KafkaProducer(producerProperties, new StringSerializer(), new JacksonSerializer());
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        log.info("shutdown kafka");
        this.kafkaProducer.close();
    }


    @JsonView({SpatialRecordView.Identity.class})
    public void publish(SpatialOperation spatialOperation, Object spatialEntity) {
        this.publish(spatialOperation, this.objectMapper.convertValue(spatialEntity, SpatialEntity.class));
    }

    @JsonView({SpatialRecordView.Identity.class})
    public void publish(final SpatialOperation spatialOperation, final SpatialEntity spatialEntity) {

        this.idTrackerService.addId(spatialEntity.getId());

        publishExecutor.submit(() -> {
            LocationInformation locationInformation;

            //todo: remove
            try{
                //if purple tiger is offline
                locationInformation = intersectingExtensionsService.calculate(spatialEntity);
            }catch (Exception e){
                //.. use default
                log.error("purple tiger cant be reached", e);
                locationInformation = new LocationInformation();
                ArrayList<SpatialEntity> sel = new ArrayList<>();
                Area area = new Area();
                area.setId("c56b3543-6853-4d86-a7bc-1cde673a5582");
                sel.add(area);
                locationInformation.setSpatialEntities(sel);

            }

            ArrayList<String> idList = new ArrayList<>();
            for(SpatialEntity spatialEntityFromInfo : locationInformation.getSpatialEntities()){
                idList.add(spatialEntityFromInfo.getId());
            }
            spatialOperation.setIntersectingExtensions(idList);

            for(String idsToUpdate : idList){

                String messageUUID = UUID.randomUUID().toString();
                this.idTrackerService.addId(messageUUID);
                KafkaProducerService.this.kafkaProducer.send(
                    new ProducerRecord<>(
                        Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX + idsToUpdate,
                        messageUUID,
                        spatialOperation
                    )
                );
            }
        });
    }
}
