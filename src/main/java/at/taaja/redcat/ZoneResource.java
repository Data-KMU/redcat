package at.taaja.redcat;

import at.taaja.redcat.model.AbstractExtension;
import at.taaja.redcat.model.Area;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/")
@Produces(MediaType.APPLICATION_JSON)
public class ZoneResource {

    @Inject
    ZoneRepository zoneRepository;

    @GET
    @Path("extension/{id}")
    public AbstractExtension getExtension(@PathParam("id") String extensionId) {
        return zoneRepository.getExtension(extensionId);
    }

//    @GET
//    @Path("test")
//    public AbstractExtension getTest() {
//        return new Area();
//    }



//    @POST
//    @Path("extension")
//    public Response addExtension(AbstractExtension abstractExtension) {
//        this.zoneRepository.insertExtension(abstractExtension);
//        return Response.ok().build();
//    }


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
//        AbstractExtension abstractExtension = objectMapper.convertValue(updatedExtension, AbstractExtension.class);
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