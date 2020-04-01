package at.taaja.redcat;

import at.taaja.redcat.model.AbstractExtension;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.taaja.messaging.Topics;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

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