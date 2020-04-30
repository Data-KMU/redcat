package at.taaja.redcat;


import com.fasterxml.jackson.annotation.JsonView;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.models.views.SpatialRecordView;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/v1/extension")
@Produces(MediaType.APPLICATION_JSON)
public class ZoneResource {

    @Inject
    ZoneRepository zoneRepository;

    @Inject
    KafkaProducerService kafkaProducerService;

    @GET
    @Path("/{id}")
    public SpatialEntity getExtension(@PathParam("id") String extensionId) {
        return zoneRepository.getSpatialEntity(extensionId);
    }


    @POST
    public SpatialOperation addExtension(SpatialEntity spatialEntity) {
        this.zoneRepository.insertSpatialEntity(spatialEntity);
        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Created);
        spatialOperation.setTargetId(spatialEntity.getId());

        this.kafkaProducerService.publish(spatialOperation, spatialEntity);

        return spatialOperation;
    }

    @DELETE
    @Path("/{id}")
    @JsonView({SpatialRecordView.Identity.class})
    public SpatialOperation removeExtension(@PathParam("id") String extensionId) {
        SpatialEntity spatialEntity = zoneRepository.getSpatialEntity(extensionId);

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Removed);
        spatialOperation.setTargetId(extensionId);

        this.kafkaProducerService.publish(spatialOperation, spatialEntity);

        return spatialOperation;
    }


    @PATCH
    @Path("/{id}")
    @JsonView({SpatialRecordView.Identity.class})
    public SpatialOperation updateExtension(@PathParam("id") String extensionId) {
        SpatialEntity spatialEntity = zoneRepository.getSpatialEntity(extensionId);

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Altered);
        spatialOperation.setTargetId(extensionId);

        //todo: implement update
        this.kafkaProducerService.publish(spatialOperation, spatialEntity);

        return spatialOperation;
    }


//    @SneakyThrows
//    @POST
//    @Path("test")
//    public Response test(String s){
//        ObjectMapper objectMapper = new ObjectMapper();
//        ObjectReader objectReader = objectMapper.reader();
//
//        String topic = "spatial-life-data-c56b3543-6853-4d86-a7bc-1cde673a5582";
//
//        String id = this.getIdFromTopic(topic);
//
//        Object extension = this.zoneRepository.getExtensionAsObject(id);
//
//        ObjectReader updater = objectMapper.readerForUpdating(extension);
//        Object updatedExtension = updater.readValue(s);
//
//        SpatialEntity spatialEntity = objectMapper.convertValue(updatedExtension, SpatialEntity.class);
//
//        this.zoneRepository.update(id, updatedExtension);
//
//        return Response.ok().build();
//    }
//
//    private String getIdFromTopic(String topic){
//        return topic.substring(Topics.SPATIAL_EXTENSION_LIFE_DATA_TOPIC_PREFIX.length());
//    }

}