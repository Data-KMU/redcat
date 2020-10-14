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
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@JBossLog
public class DataMergeService  {

    public static final String MODIFIED = "modified";

    private ObjectMapper objectMapper;
    private ExecutorService taskExecutor;

    @Inject
    ExtensionObjectRepository extensionObjectRepository;

    void onStart(@Observes StartupEvent ev) {
        this.objectMapper = new ObjectMapper();
        this.taskExecutor = Executors.newCachedThreadPool();
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        this.taskExecutor.shutdown();
    }

    public Future<Object> processUpdate(String extensionId, String rawJSON) {
        return this.taskExecutor.submit(new MergeTask(extensionId, rawJSON));
    }

    public Future<Object> processUpdate(ConsumerRecord<String, String> record) {
        return this.taskExecutor.submit(
                new MergeTask(
                        this.getIdFromTopic(record.topic()),
                        record.value()
                )
        );
    }

    private final String getIdFromTopic(String topic){
        return topic.substring(Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX.length());
    }

    @Getter
    private class MergeTask implements Callable<Object>{
        private final String id;
        private final String value;


        public MergeTask(String extensionId, String rawJSON){
            this.id = extensionId;
            this.value = rawJSON;
        }

        @Override
        public Object call() throws Exception {
            log.info("update extension " + id);

            Object extension = extensionObjectRepository.findByIdOrException(id);

            //check if objekt has changed in meta

            //fix
            int coordinatesCount = 0;
            try{
                coordinatesCount =  ((List)((Map)extension).get("coordinates")).size();
            }catch (NullPointerException npe){
                //nothing
            }

            ObjectReader updater = objectMapper.readerForUpdating(extension);
            Object updatedExtension = updater.readValue(value);

            this.updateCoordinates(coordinatesCount, updatedExtension);

            this.addOrCheckModify(updatedExtension);

            //parse to validate
            SpatialEntity spatialEntity = objectMapper.convertValue(updatedExtension, SpatialEntity.class);

            if(! id.equals(spatialEntity.getId())){
                throw new Exception("Id change is not allowed new id: " + spatialEntity.getId() + ", old id: " + id);
            }

            extensionObjectRepository.update(id, updatedExtension);

            return updatedExtension;
        }

        private void updateCoordinates(int coordinatesCount, Object updatedExtension) {
            try{
                Map<String, Object> updatedExtensionMap = (Map)updatedExtension;

                List co1 = (List)updatedExtensionMap.get("coordinates");

                if(co1.size() != coordinatesCount && coordinatesCount > 0){
                    co1.remove(0);
                }
            }catch (Exception e){
                log.warn("cant updare coordinates " + e.getMessage(), e);
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


    }


}