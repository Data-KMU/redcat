package at.taaja.redcat;


import com.fasterxml.jackson.annotation.JsonView;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.models.views.SpatialRecordView;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/v1/extension")
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionResource {

    @Inject
    ExtensionRepository extensionRepository;

    @Inject
    ExtensionObjectRepository extensionObjectRepository;

    @Inject
    KafkaProducerService kafkaProducerService;

    @Inject
    DataMergeService dataMergeService;

    /**
     * @param extensionId
     * @return a spatial entity with a given ID oder 404 if nothing was found
     */
    @GET
    @Path("/{id}")
    public Object getExtension(@PathParam("id") String extensionId) {
        return extensionObjectRepository.findByIdOrException(extensionId);
    }

    /**
     * Creates a Spatial Entity
     * @param spatialEntity
     * @return a Spatial Operation with "created" as operation type
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public SpatialOperation addExtension(SpatialEntity spatialEntity) {

        //set a UUID
        spatialEntity.setId(UUID.randomUUID().toString());

        this.extensionRepository.insertOne(spatialEntity);

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Created);
        spatialOperation.setTargetId(spatialEntity.getId());

        this.kafkaProducerService.publish(spatialOperation, spatialEntity);

        return spatialOperation;
    }

    /**
     * Deletes a spatial entity with the given id and returns a spatial operation with "removed" as type OR
     * 404 if the no entity with the given id was found
     *
     * @param extensionId
     * @return
     */
    @DELETE
    @Path("/{id}")
    @JsonView({SpatialRecordView.Identity.class})
    public SpatialOperation removeExtension(@PathParam("id") String extensionId) {
        SpatialEntity spatialEntity = extensionRepository.deleteOneByIdAndGet(extensionId);

        if(spatialEntity == null){
            throw new NotFoundException("Entity not found");
        }

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Removed);
        spatialOperation.setTargetId(extensionId);

        this.kafkaProducerService.publish(spatialOperation, spatialEntity);

        return spatialOperation;
    }


//    @PATCH
//    @Path("/{id}")
//    @SneakyThrows
//    @Consumes(MediaType.APPLICATION_JSON)
//    public SpatialDataUpdate updateVehicleDate(@PathParam("id") String extensionId, @PathParam("id") String entityId, String rawJSON){
//
//        Object updatedSpatialEntity = this.kafkaDataConsumerService.processUpdate(new DataMergeService(extensionId, entityId, rawJSON)).get();
//
//        SpatialDataUpdate spatialDataUpdate = new SpatialDataUpdate();
//
//
//        return null;
//    }

    /**
     * Updates a SpatialEntity
     * It does not matter if there is an update on the Entity itself or of one of its entity it always returns a Spatial Operation
     * EXCEPT when there is no entity with the specified id.
     *
     * @param extensionId
     * @param rawJSON
     * @return
     */
    @PATCH
    @Path("/{id}")
    @SneakyThrows
    @JsonView({SpatialRecordView.Identity.class})
    @Consumes(MediaType.APPLICATION_JSON)
    public SpatialOperation updateExtension(@PathParam("id") String extensionId, String rawJSON) {

        Object updatedSpatialEntity = this.dataMergeService.processUpdate(extensionId, rawJSON).get();

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Altered);
        spatialOperation.setTargetId(extensionId);

        this.kafkaProducerService.publish(spatialOperation, updatedSpatialEntity);

        return spatialOperation;
    }


}