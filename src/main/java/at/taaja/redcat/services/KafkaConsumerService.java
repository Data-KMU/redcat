package at.taaja.redcat.services;

import io.taaja.services.AbstractKafkaConsumerService;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@JBossLog
@ApplicationScoped
public class KafkaConsumerService extends AbstractKafkaConsumerService {


    @Inject
    DataValidationAndMergeService dataValidationAndMergeService;

    @Inject
    KafkaProducerService kafkaProducerService;


    @Override
    protected void processRecord(ConsumerRecord<String, String> record) {

        if(!record.key().startsWith(kafkaProducerService.getOriginID())){
            this.dataValidationAndMergeService.processKafkaUpdate(record);
        }
    }

    /**
     * Use only groupName in groupId to allow clustering
     * @param clientId
     * @param groupName
     * @return
     */
    @Override
    protected String getGroupId(String clientId, String groupName) {
        return groupName;
    }
}
