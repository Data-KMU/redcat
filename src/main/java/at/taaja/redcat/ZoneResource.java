package at.taaja.redcat;


import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.record.spatial.SpatialEntity;

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
        return zoneRepository.getExtension(extensionId);
    }


    @POST
    public SpatialOperation addExtension(SpatialEntity spatialEntity) {
        this.zoneRepository.insertExtension(spatialEntity);
        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Created);
        spatialOperation.setTargetId(spatialEntity.getId());
        this.kafkaProducerService.publish(spatialOperation);
        return spatialOperation;
    }

    @DELETE
    @Path("/{id}")
    public SpatialOperation removeExtension(@PathParam("id") String extensionId) {
        this.zoneRepository.removeExtension(extensionId);

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Removed);
        spatialOperation.setTargetId(extensionId);
        this.kafkaProducerService.publish(spatialOperation);

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