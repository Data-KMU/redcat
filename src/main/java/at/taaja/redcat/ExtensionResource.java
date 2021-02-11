package at.taaja.redcat;


import at.taaja.redcat.repositories.ExtensionAsObjectRepository;
import at.taaja.redcat.repositories.ExtensionRepository;
import at.taaja.redcat.services.DataValidationAndMergeService;
import at.taaja.redcat.services.IntersectingExtensionsService;
import at.taaja.redcat.services.KafkaProducerService;
import com.fasterxml.jackson.annotation.JsonView;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialEntityOperation;
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

@JBossLog
@Path("/v1/extension")
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
    public Object getExtension(@PathParam("id") String extensionId) {
        return extensionAsObjectRepository.findByIdOrException(extensionId);
    }


    /**
     * Creates a Spatial Entity
     * @param rawBody
     * @return a Spatial Operation with "created" as operation type
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates a new SpatialEntity",
            description = "Creates a new Entity and persists it in the DB. A stated ID is overwritten by server. The returned SpatialEntityOperation is also published to Kafka")
    public SpatialEntityOperation addExtension(String rawBody) {
        SpatialEntity spatialEntityFromString = this.dataValidationAndMergeService.checkRawInputAndStructure(rawBody, null, SpatialEntity.class);

        spatialEntityFromString.setId(UUID.randomUUID().toString());
        extensionAsObjectRepository.insertOne(spatialEntityFromString);

        LocationInformation intersecting = this.intersectingExtensionsService.calculate(spatialEntityFromString);

        SpatialEntityOperation SpatialEntityOperation = new SpatialEntityOperation();
        SpatialEntityOperation.setOperationType(OperationType.Created);
        SpatialEntityOperation.setTargetId(intersecting.getOriginator().getId());
        this.kafkaProducerService.publish(SpatialEntityOperation, intersecting.getSpatialEntities());

        return SpatialEntityOperation;


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
            description = "Deletes the SpatialEntity with the given id. The resulting SpatialEntityOperation is also published to kafka")
    public SpatialEntityOperation removeExtension(@PathParam("id") String extensionId) {
        SpatialEntity spatialEntity = this.extensionRepository.deleteOneByIdAndGet(extensionId);
        if(spatialEntity == null){
            throw new NotFoundException("entity not found");
        }

        LocationInformation intersecting = this.intersectingExtensionsService.calculate(spatialEntity);

        SpatialEntityOperation SpatialEntityOperation = new SpatialEntityOperation();
        SpatialEntityOperation.setOperationType(OperationType.Removed);
        SpatialEntityOperation.setTargetId(intersecting.getOriginator().getId());
        this.kafkaProducerService.publish(SpatialEntityOperation, intersecting.getSpatialEntities());

        return SpatialEntityOperation;
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
                    "Returns a SpatialEntityOperation. Note: changes in the fields actuators, sampler and sensors are ignored. " +
                    "If a Id is stated in the, it must fit the id stated in the path")
    public SpatialEntityOperation updateMetaData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){
        LinkedHashMap<String, Object> parsedJson = this.dataValidationAndMergeService.checkRawInput(rawBody, entityId);


        SpatialEntityOperation SpatialEntityOperation = new SpatialEntityOperation();
        SpatialEntityOperation.setTargetId(entityId);

        //merge
        SpatialEntity changedSpatialEntity = this.dataValidationAndMergeService.processMetaUpdate(entityId, parsedJson);

        if(changedSpatialEntity != null){
            SpatialEntityOperation.setOperationType(OperationType.Altered);
            LocationInformation intersecting = this.intersectingExtensionsService.calculate(changedSpatialEntity);
            this.kafkaProducerService.publish(SpatialEntityOperation, intersecting.getSpatialEntities());
        }else{
            SpatialEntityOperation.setOperationType(OperationType.Unchanged);
        }

        return SpatialEntityOperation;
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
    public SpatialDataUpdate updateTrafficData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){

        //check
        LinkedHashMap<String, Object> parsedJson = this.dataValidationAndMergeService.checkRawInput(rawBody, entityId);

        SpatialDataUpdate spatialDataUpdate = this.dataValidationAndMergeService.processDataUpdate(entityId, parsedJson);

        if(spatialDataUpdate != null){
            this.kafkaProducerService.publish(spatialDataUpdate, entityId);
        }

        return spatialDataUpdate;


    }




}