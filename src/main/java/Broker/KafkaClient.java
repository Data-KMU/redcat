package Broker;

import Interfaces.Service;
import Utils.Settings;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.Properties;

public class KafkaClient implements Service {

    public static final Integer MESSAGE_COUNT=1000;
    public static final String CLIENT_ID="client1";
    public static final String TOPIC_NAME="test";
    public static final String GROUP_ID_CONFIG="consumerGroup1";
    public static final Integer MAX_NO_MESSAGE_FOUND_COUNT=100;
    public static final String OFFSET_RESET_LATEST="latest";
    public static final String OFFSET_RESET_EARLIER="earliest";
    public static final Integer MAX_POLL_RECORDS=1;


    private static KafkaClient ourInstance = new KafkaClient();

    public static KafkaClient getInstance() {
        return ourInstance;
    }

    private KafkaClient(){
    }


    @Override
    public void openConnection() {

        Settings settings = Settings.getInstance();
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,settings.get("kafka.brokers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaClient.GROUP_ID_CONFIG);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, KafkaClient.MAX_POLL_RECORDS);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaClient.OFFSET_RESET_EARLIER);

        Consumer<String, String> consumer = new KafkaConsumer(props);
        consumer.subscribe(Collections.singletonList(KafkaClient.TOPIC_NAME));
        int noMessageFound = 0;
        while (true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            if (consumerRecords.count() == 0) {
                noMessageFound++;
                if (noMessageFound > 100)
                    // If no message found count is reached to threshold exit loop.
                    break;
                else
                    continue;
            }
            //print each record.
            consumerRecords.forEach(record -> {
                System.out.println("Record Key " + record.key());
                System.out.println("Record value " + record.value());
                System.out.println("Record partition " + record.partition());
                System.out.println("Record offset " + record.offset());
            });
            // commits the offset of record to broker.
            consumer.commitAsync();
        }


    }

    @Override
    public void close() throws Exception {

    }
}
