package at.taaja.redcat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.Topics;
import io.taaja.models.record.spatial.SpatialEntity;
import lombok.Getter;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

@ApplicationScoped
@JBossLog
public class KafkaDataConsumerService {

    public static final String MODIFIED = "modified";


    @Inject
    IdTrackerService idTrackerService;

    @Inject
    ExtensionObjectRepository extensionObjectRepository;

    @ConfigProperty(name = "kafka.bootstrap-servers")
    private String bootstrapServers;

    @ConfigProperty(name = "kafka.poll-records")
    private int pollRecords;

    @ConfigProperty(name = "kafka.auto-commit")
    private boolean autoCommit;

    @ConfigProperty(name = "kafka.offset-reset")
    private String offsetReset;

    @ConfigProperty(name = "kafka.group-id")
    private String groupId;

    private ExtensionLivDataConsumer livDataConsumer;
    private ExecutorService taskExecutor;

    private ObjectMapper objectMapper;

    @Getter
    private class DataMergeTask implements Callable<Object> {

        private final String value;
        private final String id;
        private String messageKey = null;

        public DataMergeTask(ConsumerRecord<String, String> record) {
            this.value = record.value();
            this.id = getIdFromTopic(record.topic());
            this.messageKey = record.key();
        }

        public DataMergeTask(String id, String value){
            this.value = value;
            this.id = id;
        }

        @Override
        public Object call() throws Exception {
            log.info("update extension " + id);

            Object extension = KafkaDataConsumerService.this.extensionObjectRepository.findByIdOrException(id);

            //fix
            int coordinatesCount =  ((List)((Map)extension).get("coordinates")).size();

            ObjectReader updater = objectMapper.readerForUpdating(extension);
            Object updatedExtension = updater.readValue(value);

            this.updateCoordinates(coordinatesCount, updatedExtension);

            this.addOrCheckModify(updatedExtension);

            //parse to validate
            SpatialEntity spatialEntity = objectMapper.convertValue(updatedExtension, SpatialEntity.class);

            if(! id.equals(spatialEntity.getId())){
                throw new Exception("Id change is not allowed new id: " + spatialEntity.getId() + ", old id: " + id);
            }

            KafkaDataConsumerService.this.extensionObjectRepository.update(id, updatedExtension);

            return updatedExtension;
        }

        private void updateCoordinates(int coordinatesCount, Object updatedExtension) {
            Map<String, Object> updatedExtensionMap = (Map)updatedExtension;

            List co1 = (List)updatedExtensionMap.get("coordinates");

            if(co1.size() != coordinatesCount){
                co1.remove(0);
            }

        }

        private void addOrCheckModify(Object rawSpatialEntity) {

            Map<String, Object> root = (Map)rawSpatialEntity;

            //root level
            for (Object data : Lists.newArrayList(
                    root.get("actuators"),
                    root.get("sensors"),
                    root.get("samplers")
            )){
                try{
                    //Map: vehicleId, vehicleData
                    Map<String, Object> vehicleData = (Map)data;

                    //for Entries (id -> Data)
                    for (Object entry : vehicleData.values()){

                        //vehicle properties (data)
                        Map<String, Object> dataEntry = (Map) entry;

                        if(! dataEntry.containsKey(MODIFIED)){
                            dataEntry.put(MODIFIED, new Date());
                        }
                    }
                }catch (Exception e){
                    log.info("cant update modified " + e.getMessage(), e);
                }
            }
        }

        private final String getIdFromTopic(String topic){
            return topic.substring(Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX.length());
        }

    }

    private class ExtensionLivDataConsumer extends Thread implements Closeable {

        private volatile boolean running = true;
        private final Properties consumerProperties = new Properties();

        private ExtensionLivDataConsumer() {
            consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaDataConsumerService.this.bootstrapServers);
            consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, KafkaDataConsumerService.this.pollRecords);
            consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, KafkaDataConsumerService.this.autoCommit);
            consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaDataConsumerService.this.offsetReset);
            consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaDataConsumerService.this.groupId);
            consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, KafkaDataConsumerService.this.groupId + "-" + UUID.randomUUID().toString());
            this.setName(this.getClass().getSimpleName());
        }

        @Override
        public void run() {
            KafkaConsumer kafkaConsumer = new KafkaConsumer(this.consumerProperties, new StringDeserializer(), new StringDeserializer());
            kafkaConsumer.subscribe(Pattern.compile(Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX + ".*"));
            while (this.running){
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for(ConsumerRecord<String, String> record : records){

                    if(!KafkaDataConsumerService.this.idTrackerService.containsId(record.key())){
                        KafkaDataConsumerService.this.taskExecutor.submit(new DataMergeTask(record));
                    }

                }
            }
            kafkaConsumer.close();
        }

        @Override
        public void close() throws IOException {
            this.running = false;
        }
    }


    void onStart(@Observes StartupEvent ev) {
        this.objectMapper = new ObjectMapper();
        this.taskExecutor = Executors.newCachedThreadPool();
        this.livDataConsumer = new ExtensionLivDataConsumer();
        this.livDataConsumer.start();
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        this.livDataConsumer.close();
        this.taskExecutor.shutdown();
    }

    public Future<Object> processUpdate(String extensionId, String rawJson){
         return this.taskExecutor.submit(new DataMergeTask(extensionId, rawJson));
    }

}
