package at.taaja.redcat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ZoneResource {

    @GET
    @Path("area/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getArea() {
        return "area";
    }


    @GET
    @Path("corridor/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCorridor() {
        return "corridor";
    }

}