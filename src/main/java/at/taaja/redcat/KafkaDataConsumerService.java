package at.taaja.redcat;

import at.taaja.redcat.model.AbstractExtension;
import at.taaja.redcat.model.Area;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.messaging.Topics;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@ApplicationScoped
@JBossLog
public class KafkaDataConsumerService {

    public static final String MODIFIED = "modified";

    @Inject
    ZoneRepository zoneRepository;

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
    private ExecutorService executor;

    private ObjectMapper objectMapper;

    private class KafkaRecordHandler implements Runnable {

        private final ConsumerRecord<Long, String> record;

        public KafkaRecordHandler(ConsumerRecord<Long, String> record) {
            this.record = record;
        }

        @Override
        public void run() {

            try {
                String id = this.getIdFromTopic(record.topic());

                log.info("update extension " + id);

                AbstractExtension extension = KafkaDataConsumerService.this.zoneRepository.getExtension(id);

                if(extension == null){
                    if("c56b3543-6853-4d86-a7bc-1cde673a5582".equals(id)){
                        //add new default area
                        Area area = new Area();
                        area.setId("c56b3543-6853-4d86-a7bc-1cde673a5582");
                        KafkaDataConsumerService.this.zoneRepository.addExtension(area);
                    }else{
                        throw new NullPointerException("Extension cant be found. id: " + id);
                    }
                }

                ObjectReader updater = objectMapper.readerForUpdating(extension);
                Object updatedExtension = updater.readValue(record.value());

                //parse to validate
                AbstractExtension abstractExtension = objectMapper.convertValue(updatedExtension, AbstractExtension.class);

                if(! id.equals(abstractExtension.getId())){
                    throw new Exception("Id change is not allowed new id: " + abstractExtension.getId() + ", old id: " + id);
                }

                this.addOrCheckModify(abstractExtension);

                KafkaDataConsumerService.this.zoneRepository.update(id, updatedExtension);

            } catch (Exception e) {
                log.error("cant parse object: " + e.getMessage(), e);
            }
        }

        private void addOrCheckModify(AbstractExtension abstractExtension) {

            long now = System.currentTimeMillis();
            long threshold = 5000; // 5sek

            //root level
            for (Object data : Lists.newArrayList(
                    abstractExtension.getActuators(),
                    abstractExtension.getSamplers(),
                    abstractExtension.getSensors())
            ){
                try{
                    //Map: vehicleId, vehicleData
                    Map<String, Object> vehicleData = (Map)data;

                    //for Entries (id -> Data)
                    for (Map.Entry<String, Object> entry : vehicleData.entrySet()){

                        //vehicle properties (data)
                        Map<String, Object> dataEntry = (Map) entry.getValue();

                        if(! dataEntry.containsKey(MODIFIED)){
                            dataEntry.put(MODIFIED, new Date());
                        }
                    }
                }catch (Exception e){
                    log.error("cant update modified " + e.getMessage(), e);
                }
            }
        }

        private String getIdFromTopic(String topic){
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
            KafkaConsumer kafkaConsumer = new KafkaConsumer(this.consumerProperties, new LongDeserializer(), new StringDeserializer());
            kafkaConsumer.subscribe(Pattern.compile(Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX + ".*"));
            while (this.running){
                ConsumerRecords<Long, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for(ConsumerRecord<Long, String> record : records){
                    KafkaDataConsumerService.this.executor.submit(new KafkaRecordHandler(record));
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
        this.executor = Executors.newCachedThreadPool();
        this.livDataConsumer = new ExtensionLivDataConsumer();
        this.livDataConsumer.start();
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        this.livDataConsumer.close();
        this.executor.shutdown();
    }

}