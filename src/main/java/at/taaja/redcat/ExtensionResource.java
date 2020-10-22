package at.taaja.redcat;


import at.taaja.redcat.repositories.ExtensionObjectRepository;
import at.taaja.redcat.services.DataValidationAndMergeService;
import at.taaja.redcat.services.IntersectingExtensionsService;
import at.taaja.redcat.services.KafkaProducerService;
import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.views.SpatialRecordView;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/v1/extension")
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionResource {


    @Inject
    ExtensionObjectRepository extensionObjectRepository;

    @Inject
    DataValidationAndMergeService dataValidationAndMergeService;

    @Inject
    IntersectingExtensionsService intersectingExtensionsService;

    @Inject
    KafkaProducerService kafkaProducerService;

    /**
     * @param extensionId
     * @return a spatial entity with a given ID oder 404 if nothing was found
     */
    @GET
    @Path("/{id}")
    public Uni<Object> getExtension(@PathParam("id") String extensionId) {
            return Uni.createFrom().item(extensionId).onItem().apply(id -> extensionObjectRepository.findByIdOrException(id));
    }


    /**
     * Creates a Spatial Entity
     * @param rawBody
     * @return a Spatial Operation with "created" as operation type
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<SpatialOperation> addExtension(String rawBody) {

        return Uni.createFrom().item(rawBody)

                //check input
                .onItem().apply(s -> this.dataValidationAndMergeService.checkSpatialEntityForPost(s))

                //persist and retrieve intersecting entities
                .onItem().apply(spatialEntity -> {
                    extensionObjectRepository.insertOne(spatialEntity);
                    return this.intersectingExtensionsService.calculate(spatialEntity);
                })

                //publish
                .onItem().apply(locationInformation -> {
                    SpatialOperation spatialOperation = new SpatialOperation();
                    spatialOperation.setOperationType(OperationType.Created);
                    spatialOperation.setTargetId(locationInformation.getOriginator().getId());

                    this.kafkaProducerService.publish(spatialOperation, locationInformation.getSpatialEntities());

                    return spatialOperation;
                });

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
//        SpatialEntity spatialEntity = extensionRepository.deleteOneByIdAndGet(extensionId);
//
//        if(spatialEntity == null){
//            throw new NotFoundException("Entity not found");
//        }
//
//        SpatialOperation spatialOperation = new SpatialOperation();
//        spatialOperation.setOperationType(OperationType.Removed);
//        spatialOperation.setTargetId(extensionId);
//
//        this.kafkaProducerService.publish(spatialOperation, spatialEntity);
//
//        return spatialOperation;
        return null;
    }


    /**
     * Updates the meta info of a SpatialEntity
     *
     * @param extensionId
     * @param rawJSON
     * @return
     */
    @PUT
    @Path("/{id}")
    @SneakyThrows
    @JsonView({SpatialRecordView.Identity.class})
    @Consumes(MediaType.APPLICATION_JSON)
    public SpatialDataUpdate updateMetaData(@PathParam("id") String extensionId, @PathParam("id") String entityId, String rawBody){

//        Object updatedSpatialEntity = this.kafkaDataConsumerService.processUpdate(new DataMergeService(extensionId, entityId, rawJSON)).get();
//
//        SpatialDataUpdate spatialDataUpdate = new SpatialDataUpdate();


        return null;
    }

    /**
     * Updates a SpatialEntity with new traffic data
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
    public SpatialOperation updateExtension(@PathParam("id") String extensionId, String rawBody) {

//        Object updatedSpatialEntity = this.dataValidationAndMergeService.processUpdate(extensionId, rawJSON).get();
//
//        SpatialOperation spatialOperation = new SpatialOperation();
//        spatialOperation.setOperationType(OperationType.Altered);
//        spatialOperation.setTargetId(extensionId);
//
//        this.kafkaProducerService.publish(spatialOperation, updatedSpatialEntity);
//
//        return spatialOperation;


        return null;
    }


}