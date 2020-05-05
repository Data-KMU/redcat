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
    KafkaDataConsumerService kafkaDataConsumerService;

    @GET
    @Path("/{id}")
    public Object getExtension(@PathParam("id") String extensionId) {
        return extensionObjectRepository.findByIdOrException(extensionId);
    }

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


    @PATCH
    @Path("/{id}")
    @SneakyThrows
    @JsonView({SpatialRecordView.Identity.class})
    @Consumes(MediaType.APPLICATION_JSON)
    public SpatialOperation updateExtension(@PathParam("id") String extensionId, String spatialDataUpdate) {

        Object updatedSpatialEntity = this.kafkaDataConsumerService.processUpdate(extensionId, spatialDataUpdate).get();

        SpatialOperation spatialOperation = new SpatialOperation();
        spatialOperation.setOperationType(OperationType.Altered);
        spatialOperation.setTargetId(extensionId);

        this.kafkaProducerService.publish(spatialOperation, updatedSpatialEntity);

        return spatialOperation;
    }


}