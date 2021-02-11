package at.taaja.redcat;


import io.taaja.models.generic.Coordinates;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.message.data.update.SpatialDataUpdate;
import io.taaja.models.message.extension.operation.OperationType;
import io.taaja.models.message.extension.operation.SpatialEntityOperation;
import io.taaja.models.record.spatial.*;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.*;

//@Path("/dev")
//@Produces(MediaType.APPLICATION_JSON)
//@ApplicationScoped
public class TestResource {
//
//    private static String errMsg = "use /dev/example/{Area|Corridor|SpatialEntity|SpatialDataUpdate|SpatialEntityOperation|LocationInformation} as query param";
//
//    @GET
//    @Path("/example/{className}")
//    @Operation(summary = "Returns an example of the stated class",
//            description = "Returns the JSON of a example instance of the given Type")
//    public Object test(@PathParam("className") String className){
//
//        if(className == null){
//            return errMsg;
//        }
//
//        Coordinates coordinates = new Coordinates();
//        coordinates.setLatitude(1000f);
//        coordinates.setLatitude(11.12f);
//        coordinates.setLongitude(46.34f);
//        SpatialDataUpdate spatialDataUpdate = new SpatialDataUpdate();
//        spatialDataUpdate.setVehicleInformation()
//        .addActuatorData(UUID.randomUUID().toString(), positionUpdate);
//        Map<String, Object> actuators = spatialDataUpdate.getActuators();
//
//
//        Corridor corridor = new Corridor();
//        corridor.setId(UUID.randomUUID().toString());
//        corridor.setShape(Corridor.ShapeType.Circular);
//        corridor.setValidFrom(new Date());
//        corridor.setValidUntil(new Date());
//        corridor.setPriority(ExtensionPriority.VERY_HIGH_PRIORITY);
//
//
//        List<Waypoint> waypoints = new ArrayList<>();
//
//        Waypoint waypoint = new Waypoint();
//        waypoint.setAltitude(1000f);
//        waypoint.setLatitude(11.12f);
//        waypoint.setLongitude(46.34f);
//        waypoint.setAdditionalData("additional data");
//        waypoints.add(waypoint);
//
//        corridor.setCoordinates(waypoints);
//        corridor.setActuators(actuators);
//
//        Area area = new Area();
//        area.setId(UUID.randomUUID().toString());
//        area.setElevation(123f);
//        area.setHeight(456f);
//        area.setValidFrom(new Date());
//        area.setValidUntil(new Date());
//        area.setPriority(ExtensionPriority.HIGH_PRIORITY);
//        List<List<LongLat>> lll = new ArrayList<>();
//        List<LongLat> longlats = new ArrayList<>();
//        lll.add(longlats);
//        LongLat longLat = new LongLat();
//        longLat.setLatitude(11.11f);
//        longLat.setLongitude(46.46f);
//        longlats.add(longLat);
//        longlats.add(longLat);
//        longlats.add(longLat);
//        area.setCoordinates(lll);
//        area.setActuators(actuators);
//
//
//
//        switch (className.toLowerCase()){
//            case "spatialentity":
//            case "area":
//                return area;
//
//            case "corridor":
//                return corridor;
//
//            case "spatialdataupdate":
//                return spatialDataUpdate;
//
//            case "SpatialEntityOperation":
//                SpatialEntityOperation SpatialEntityOperation = new SpatialEntityOperation();
//                SpatialEntityOperation.setIntersectingExtensions(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
//                SpatialEntityOperation.setTargetId(UUID.randomUUID().toString());
//                SpatialEntityOperation.setOperationType(OperationType.Created);
//
//                return SpatialEntityOperation;
//
//            case "locationinformation":
//
//                LocationInformation locationInformation = new LocationInformation();
//                List<SpatialEntity> spatialEntities = locationInformation.getSpatialEntities();
//                spatialEntities.add(corridor);
//                area.setActuators(null);
//                spatialEntities.add(area);
//                locationInformation.setAltitude(123f);
//                locationInformation.setLatitude(456f);
//                locationInformation.setLongitude(789f);
//
//                return locationInformation;
//
//            default:
//                return errMsg;
//        }
//    }


}