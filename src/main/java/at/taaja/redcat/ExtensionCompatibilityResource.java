package at.taaja.redcat;


import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.message.extension.operation.SpatialOperation;
import io.taaja.models.views.SpatialRecordView;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/v1/compatibility")
@JBossLog
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionCompatibilityResource {

    @Inject
    ExtensionResource extensionResource;

    @GET
    @Path("/getExtension/{id}")
    @Operation(summary = "see GET /v1/extension/{id}")
    public Uni<Object> getExtension(@PathParam("id") String extensionId) {
        return this.extensionResource.getExtension(extensionId);
    }

    @POST
    @Path("/createExtension")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "see POST /v1/extension")
    public Uni<SpatialOperation> addExtension(String rawBody) {
        return this.extensionResource.addExtension(rawBody);
    }


    @GET
    @Path("/deleteExtension/{id}")
    @Operation(summary = "see DELETE /v1/extension/{id}")
    public Uni<SpatialOperation> removeExtension(@PathParam("id") String extensionId) {
        return this.extensionResource.removeExtension(extensionId);
    }



    @POST
    @Path("/updateMetaInformation/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "see PUT /v1/extension/{id}")
    public Uni<SpatialOperation> updateMetaData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){
       return this.extensionResource.updateMetaData(entityId, rawBody);
    }

    @POST
    @Path("/updateVehicleInformation/{id}")
    @JsonView({SpatialRecordView.Full.class})
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "see PATCH /v1/extension/{id}")
    public Uni<SpatialDataUpdate> updateTrafficData(
            final @PathParam("id") String entityId,
            final String rawBody
    ){
        return this.extensionResource.updateTrafficData(entityId, rawBody);
    }


}