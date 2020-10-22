package at.taaja.redcat.services;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.Topics;
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
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
@JBossLog
public class KafkaDataConsumerService {

    @Inject
    DataValidationAndMergeService dataValidationAndMergeService;

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


                    if(!record.key().startsWith(KafkaProducerService.originatorId)){
                        KafkaDataConsumerService.this.dataValidationAndMergeService.processUpdate(record);
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
        this.livDataConsumer = new ExtensionLivDataConsumer();
        this.livDataConsumer.start();
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        this.livDataConsumer.close();
    }

}
