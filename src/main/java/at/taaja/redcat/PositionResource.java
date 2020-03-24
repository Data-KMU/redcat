package at.taaja.redcat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/theBox/v1/position")
public class PositionResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPosition() {
        return "hello";
    }


}