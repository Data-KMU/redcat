package io.taaja.redcat.services;


import io.taaja.redcat.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;



/**
 * https://dzone.com/articles/magic-of-kafka-with-spring-boot
 */
@Service
public class KafkaSubscriberService {

    Logger logger = LoggerFactory.getLogger(KafkaSubscriberService.class);

    @KafkaListener(topics = Constants.TOPIC_VEHICLE)
    public void posUpdate(String message){
        logger.info(String.format("Vehicle update %s",message));
    }

}
