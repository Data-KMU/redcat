package at.taaja.redcat.services;

import at.taaja.redcat.repositories.ExtensionAsObjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.record.spatial.SpatialEntity;
import lombok.extern.jbosslog.JBossLog;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


@JBossLog
@ApplicationScoped
public class DataValidationAndMergeService {

    public static final String MODIFIED = "_modified";

    public static final Set<String> DATA_PROPERTIES = Set.of("actuators", "sensors", "samplers");

    private static class MergingContext{

        private final AtomicBoolean _changed = new AtomicBoolean(false);
        private final Set<String> _keysToIterate;
        private final long _modified;
        private final MergingContext _parentContext;


        private MergingContext(Set<String> keysToIterate) {
            this._keysToIterate = keysToIterate;
            this._modified = Instant.now().getEpochSecond();
            this._parentContext = null;
        }

        private MergingContext(MergingContext parentContext, Set<String> keysToIterate) {
            this._keysToIterate = keysToIterate;
            this._modified = parentContext._modified;
            this._parentContext = parentContext;

        }

        public boolean hasDataChanged(){
            return
                    this._parentContext == null
                            ? this._changed.get()
                            : this._changed.get() || this._parentContext.hasDataChanged() ;
        }

        public void markDataAsChanged(){
            this._changed.set(true);
            if(this._parentContext != null){
                this._parentContext.markDataAsChanged();
            }
        }

        public long getModified(){
            return this._modified;
        }

        public Iterable<String> getKeyIterable(){
            return this._keysToIterate;
        }

    }


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


    public SpatialEntity processMetaUpdate(String entityId, LinkedHashMap<String, Object> parsedJson) {

        LinkedHashMap<String, Object> original = (LinkedHashMap) this.extensionAsObjectRepository.findByIdOrException(entityId);

        Set<String> propertiesToProcess = parsedJson.keySet();
        propertiesToProcess.removeAll(DATA_PROPERTIES);
        propertiesToProcess.remove("_id");

        MergingContext mergingContext = new MergingContext(propertiesToProcess);

        this.deepMerge(mergingContext, original, parsedJson);

        SpatialEntity newSpatialEntity = null;

        if(mergingContext.hasDataChanged()){
            //parse to check
            newSpatialEntity = this.objectMapper.convertValue(original, SpatialEntity.class);
            this.extensionAsObjectRepository.update(entityId, original);
        }

        return newSpatialEntity;
    }

    public SpatialDataUpdate processDataUpdate(String entityId, LinkedHashMap<String, Object> parsedJson) {

        LinkedHashMap<String, Object> original = (LinkedHashMap) this.extensionAsObjectRepository.findByIdOrException(entityId);

        MergingContext mergingContext = new MergingContext(DATA_PROPERTIES);

        this.deepMerge(mergingContext, original, parsedJson);

        SpatialDataUpdate spatialDataUpdate = null;

        if(mergingContext.hasDataChanged()){
            original.put("messageChannel", "spatialDataUpdate");
            spatialDataUpdate = objectMapper.convertValue(original, SpatialDataUpdate.class);
            original.remove("messageChannel");
            this.extensionAsObjectRepository.update(entityId, original);
        }

        return spatialDataUpdate;
    }

    private void deepMerge(final MergingContext mergingContext, final Map<String, Object> original, final Map<String, Object> newData){

        mergingContext.getKeyIterable().forEach(key -> {

            //new data key exists / no key in old
            if(newData.containsKey(key)) {

                Object newValue = newData.get(key);

                if (original.containsKey(key)) {
                    //merge

                    Object originalValue = original.get(key);

                    //delete if null
                    if(newValue == null){
                        original.remove(key);
                        original.put(MODIFIED, mergingContext.getModified());
                        mergingContext.markDataAsChanged();
                        return;
                    }

                    if(newValue instanceof List && originalValue instanceof List){
                        // merge Array
                        List<Object> newValueList = (List) newValue;
                        List<Object> originalValueList = (List) originalValue;

                        deepMerge(mergingContext, originalValueList, newValueList);

                    } else if(newValue instanceof Map && originalValue instanceof Map){
                        // merge Map
                        Map<String, Object> newValueMap = (Map) newValue;
                        Map<String, Object> originalValueMap = (Map) originalValue;

                        deepMerge(new MergingContext(mergingContext, newValueMap.keySet()), originalValueMap, newValueMap);
                    }else {
                        //overwrite simple
                        if(!newValue.equals(originalValue)){
                            original.put(key, newData.get(key));
                            mergingContext.markDataAsChanged();
                        }
                    }
                } else {
                    //add
                    if(newValue != null){
                        original.put(key, newValue);
                        mergingContext.markDataAsChanged();
                    }
                }

                //propagate modified to root
                if(mergingContext.hasDataChanged()){
                    original.put(MODIFIED, mergingContext.getModified());
                }

            }

        });
    }

    private void deepMerge(final MergingContext mergingContext, final List<Object> original, final List<Object> newData){

        for (int i = 0; i < newData.size(); i++){
            Object currentNewListObject = newData.get(i);

            if (i == original.size()){
                original.add(currentNewListObject);
                mergingContext.markDataAsChanged();
                continue;
            }

            Object currentOriginalListObject = original.get(i);

            if (currentNewListObject instanceof Map && currentOriginalListObject instanceof Map){
                Map<String, Object> newValueMap = (Map) currentNewListObject;
                Map<String, Object> originalValueMap = (Map) currentOriginalListObject;

                deepMerge(new MergingContext(mergingContext, newValueMap.keySet()), newValueMap, originalValueMap);
            } else if(currentNewListObject instanceof List && currentOriginalListObject instanceof List){
                List<Object> newValueList = (List) currentNewListObject;
                List<Object> originalValueList = (List) currentOriginalListObject;

                deepMerge(mergingContext, newValueList, originalValueList);
            } else {

                //check if ob are null
                if(
                        (currentNewListObject == null && currentOriginalListObject != null)
                    || (currentNewListObject != null && ! currentNewListObject.equals(currentOriginalListObject))
                ){
                    original.set(i, currentNewListObject);
                    mergingContext.markDataAsChanged();
                }
            }

        }

    }

}