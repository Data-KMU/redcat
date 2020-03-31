package at.taaja.redcat;

import at.taaja.redcat.model.Area;
import at.taaja.redcat.model.Corridor;

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
    @Path("area/{id}")
    public Area getArea(@PathParam("id") String areaId) {
        return zoneRepository.getArea(areaId);
    }


    @GET
    @Path("corridor/{id}")
    public Corridor getCorridor(@PathParam("id") String corridorId) {
        return zoneRepository.getCorridor(corridorId);
    }


//    @POST
//    public Response addExtension(AbstractExtension abstractExtension) {
//
//        abstractExtension.setId(UUID.randomUUID().toString());
//        this.zoneRepository.insertExtension(abstractExtension);
//
//        return Response.ok().build();
//    }





}