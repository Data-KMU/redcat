package at.taaja.redcat;


import at.taaja.redcat.repositories.ExtensionAsObjectRepository;
import at.taaja.redcat.repositories.ExtensionRepository;
import at.taaja.redcat.services.DataValidationAndMergeService;
import at.taaja.redcat.services.IntersectingExtensionsService;
import at.taaja.redcat.services.KafkaProducerService;
import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.models.views.SpatialRecordView;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.UUID;

@Path("/v1/extension")
@JBossLog
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionResource {

    @Inject
    ExtensionAsObjectRepository extensionAsObjectRepository;

    @Inject
    ExtensionRepository extensionRepository;

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
    @Operation(summary = "Returns a Spatial Entity with the given Id",
    description = "Or returns 404 if no entity was found")
    public Uni<Object> getExtension(@PathParam("id") String extensionId) {
        return Uni.createFrom().item(extensionId).onItem().apply(id -> extensionAsObjectRepository.findByIdOrException(id));
    }


    /**
     * Creates a Spatial Entity
     * @param rawBody
     * @return a Spatial Operation with "created" as operation type
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates a new SpatialEntity",
            description = "Creates a new Entity and persists it in the DB. A stated ID is overwritten by server. The returned SpatialOperation is also published to Kafka")
    public Uni<SpatialOperation> addExtension(String rawBody) {

        return Uni.createFrom().item(rawBody)

                //check input
                .onItem().apply(s -> this.dataValidationAndMergeService.checkRawInputAndStructure(s, null, SpatialEntity.class))

                //persist and retrieve intersecting entities
                .onItem().apply(spatialEntity -> {
                    spatialEntity.setId(UUID.randomUUID().toString());
                    extensionAsObjectRepository.insertOne(spatialEntity);
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
    @Operation(summary = "Deletes a SpatialEntity",
            description = "Deletes the SpatialEntity with the given id. The resulting SpatialOperation is also published to kafka")
    public Uni<SpatialOperation> removeExtension(@PathParam("id") String extensionId) {

        return Uni.createFrom().item(extensionId)

                //find item
                .onItem().apply(id -> {
                    SpatialEntity spatialEntity = this.extensionRepository.deleteOneByIdAndGet(id);
                    if(spatialEntity == null){
                        throw new NotFoundException("entity not found");
                    }
                    return spatialEntity;
                })

                //calculate intersecting
                .onItem().apply(spatialEntity -> this.intersectingExtensionsService.calculate(spatialEntity))

                //return location info
                .onItem().apply(locationInformation -> {
                    SpatialOperation spatialOperation = new SpatialOperation();
                    spatialOperation.setOperationType(OperationType.Removed);
                    spatialOperation.setTargetId(locationInformation.getOriginator().getId());

                    this.kafkaProducerService.publish(spatialOperation, locationInformation.getSpatialEntities());
                    return spatialOperation;
                });

    }


    /**
     * Updates the meta info of a SpatialEntity
     *
     * @param entityId
     * @param rawBody
     * @return
     */
    @PUT
    @Path("/{id}")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates the Meta Information",
            description = "Updates the Meta Information (like coordinates, type, etc.) of a the SpatialEntity with the given Id. " +
                    "Returns a SpatialOperation. Note: changes in the fields actuators, sampler and sensors are ignored. " +
                    "If a Id is stated in the, it must fit the id stated in the path")
    public Uni<SpatialOperation> updateMetaData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){
        return Uni.createFrom().voidItem()

                .onItem().apply(aVoid -> {

                    //check
                    LinkedHashMap<String, Object> parsedJson = this.dataValidationAndMergeService.checkRawInput(rawBody, entityId);


                    SpatialOperation spatialOperation = new SpatialOperation();
                    spatialOperation.setTargetId(entityId);

                    //merge
                    SpatialEntity changedSpatialEntity = this.dataValidationAndMergeService.processMetaUpdate(entityId, parsedJson);

                    if(changedSpatialEntity != null){
                        spatialOperation.setOperationType(OperationType.Altered);
                        LocationInformation intersecting = this.intersectingExtensionsService.calculate(changedSpatialEntity);
                        this.kafkaProducerService.publish(spatialOperation, intersecting.getSpatialEntities());
                    }else{
                        spatialOperation.setOperationType(OperationType.Unchanged);
                    }

                    return spatialOperation;
                });

    }

    /**
     * Updates a SpatialEntity with traffic traffic data
     *
     * @param entityId
     * @param rawBody
     * @return
     */
    @PATCH
    @Path("/{id}")
    @SneakyThrows
    @JsonView({SpatialRecordView.Full.class})
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates the Traffic Information",
            description = "Updates the Traffic Information (eg. the data stored in the properties actuators, sampler and sensors). " +
                    "Changes in other properties are ignored. If a Id is stated in the, it must fit the id stated in the path")
    public Uni<SpatialDataUpdate> updateTrafficData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){

        return Uni.createFrom().voidItem()

                .onItem().apply(aVoid -> {

                    //check
                    LinkedHashMap<String, Object> parsedJson = this.dataValidationAndMergeService.checkRawInput(rawBody, entityId);

                    SpatialDataUpdate spatialDataUpdate = this.dataValidationAndMergeService.processDataUpdate(entityId, parsedJson);

                    if(spatialDataUpdate != null){
                        this.kafkaProducerService.publish(spatialDataUpdate, entityId);
                    }

                    return spatialDataUpdate;
                });

    }




}