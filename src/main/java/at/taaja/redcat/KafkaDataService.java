package at.taaja.redcat;

import io.quarkus.runtime.StartupEvent;
import io.taaja.Constants;
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
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@ApplicationScoped
@JBossLog
public class KafkaDataService {

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

    ConsumerWorker areaWorker, corridorWorker;


    private static class ConsumerWorker extends Thread implements Closeable {

        private final String topicPrefix;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Properties properties;



        private ConsumerWorker(String topicPrefix, Properties properties) {
            this.topicPrefix = topicPrefix;
            properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "client-" + UUID.randomUUID().toString());
            this.properties = properties;
            this.setName(topicPrefix + "-consumer");
        }

        @Override
        public void run() {
            KafkaConsumer kafkaConsumer = new KafkaConsumer(this.properties, new LongDeserializer(), new StringDeserializer());
            kafkaConsumer.subscribe(Pattern.compile(topicPrefix + ".*"));
            while (this.running.get()){

                ConsumerRecords<Long, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for(ConsumerRecord<Long, String> record : records){

                }

            }

            kafkaConsumer.close();

        }


        @Override
        public void close() throws IOException {
            this.running.set(false);
        }
    }


    void onStart(@Observes StartupEvent ev) {

        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.pollRecords);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, this.autoCommit);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.offsetReset);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "redcat");

        this.areaWorker = new ConsumerWorker(Constants.KAFKA_AREA_TOPIC_PREFIX, (Properties)consumerProperties.clone());
        this.corridorWorker = new ConsumerWorker(Constants.KAFKA_CORRIDOR_TOPIC_PREFIX, (Properties)consumerProperties.clone());

    }

}
