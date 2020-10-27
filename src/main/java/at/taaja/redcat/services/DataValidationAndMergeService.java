package at.taaja.redcat.services;

import at.taaja.redcat.repositories.ExtensionAsObjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.common.collect.Lists;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.kafka.Topics;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.record.spatial.SpatialEntity;
import lombok.Getter;
import lombok.extern.jbosslog.JBossLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@JBossLog
@ApplicationScoped
public class DataValidationAndMergeService {

    public static final String MODIFIED = "modified";

    public static final List<String> DATA_PROPERTIES =   Arrays.asList("actuators", "sensors", "samplers");

    private ObjectMapper objectMapper;
    private ExecutorService taskExecutor;

    @Inject
    ExtensionAsObjectRepository extensionAsObjectRepository;

    void onStart(@Observes StartupEvent ev) {
        this.objectMapper = new ObjectMapper();
        this.taskExecutor = Executors.newCachedThreadPool();
    }

    void onStop(@Observes ShutdownEvent ev) throws IOException {
        this.taskExecutor.shutdown();
    }


    public <T> T checkRawInputAndStructure(String raw, String entityId, Class<T> valueType) {
        LinkedHashMap<String, Object> hm = this.checkRawInput(raw, entityId);
        return  this.objectMapper.convertValue(hm, valueType);
    }

    public LinkedHashMap<String, Object> checkRawInput(String raw, String entityId) {
        try {
            LinkedHashMap<String, Object> input = this.objectMapper.readValue(raw, LinkedHashMap.class);
            if(entityId != null && input.containsKey("_id") && ! entityId.equals(input.get("_id"))){
                throw new BadRequestException("key altering is not allowed");
            }
            return input;
        } catch (MismatchedInputException mie){
            throw new BadRequestException("cant process json array");
        } catch (JsonProcessingException jpe) {
            throw new BadRequestException("cant read json");
            //do nothing
        }
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

    public SpatialEntity processMetaUpdate(String entityId, LinkedHashMap<String, Object> parsedJson) {

        LinkedHashMap<String, Object> original = (LinkedHashMap) this.extensionAsObjectRepository.findByIdOrException(entityId);

        Set<String> propertiesToProcess = parsedJson.keySet();
        propertiesToProcess.removeAll(DATA_PROPERTIES);
        propertiesToProcess.remove("_id");

        this.deepMerge(original, parsedJson,propertiesToProcess);

        return null;
    }

    public SpatialDataUpdate processDataUpdate(String entityId, LinkedHashMap<String, Object> parsedJson) {

        LinkedHashMap<String, Object> original = (LinkedHashMap) this.extensionAsObjectRepository.findByIdOrException(entityId);

        this.deepMerge(original, parsedJson, DATA_PROPERTIES);

        return null;
    }

    private void deepMerge(final Map<String, Object> original, final Map<String, Object> newData, Iterable<String> keysToProcess){

        keysToProcess.forEach(key -> {

            //new data key exists / no key in old
            if(newData.containsKey(key)) {
                if (original.containsKey(key)) {
                    //merge

                    Object originalValue = original.get(key);
                    Object newValue = newData.get(key);

                    //delete if null
                    if(newValue == null){
                        original.remove(key);
                    }

                    if(newValue instanceof List && originalValue instanceof List){
                        // merge Array
                        List<Object> newValueList = (List) newValue;
                        List<Object> originalValueList = (List) originalValue;

                        deepMerge(originalValueList, newValueList);

                    } else if(newValue instanceof Map && originalValue instanceof Map){
                        // merge Map
                        Map<String, Object> newValueMap = (Map) newValue;
                        Map<String, Object> originalValueMap = (Map) originalValue;

                        deepMerge(originalValueMap, newValueMap, newValueMap.keySet());
                    }else {
                        //overwrite simple
                        original.put(key, newData.get(key));
                    }

                } else {
                    //add

                    Object insert = newData.get(key);
                    if(insert != null){
                        original.put(key, insert);
                    }
                }
            }

        });

    }

    private void deepMerge(final List<Object> original, final List<Object> newData){

        for (int i = 0; i < newData.size(); i++){
            Object currentNewListObject = newData.get(i);

            if(i == original.size()){
                original.add(currentNewListObject);
                continue;
            }

            Object currentOriginalListObject = original.get(i);

            if(currentNewListObject instanceof Map && currentOriginalListObject instanceof Map){
                Map<String, Object> newValueMap = (Map) currentNewListObject;
                Map<String, Object> originalValueMap = (Map) currentOriginalListObject;

                deepMerge(newValueMap, originalValueMap, newValueMap.keySet());
            }else if(currentNewListObject instanceof List && currentOriginalListObject instanceof List){
                List<Object> newValueList = (List) currentNewListObject;
                List<Object> originalValueList = (List) currentOriginalListObject;

                deepMerge(newValueList, originalValueList);
            }else {
                original.set(i, currentNewListObject);
            }


        }


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

            Object extension = extensionAsObjectRepository.findByIdOrException(id);

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

            extensionAsObjectRepository.update(id, updatedExtension);

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