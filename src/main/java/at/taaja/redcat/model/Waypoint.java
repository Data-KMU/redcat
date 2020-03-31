package at.taaja.redcat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({ "longitude", "latitude", "altitude", "additionalData" })
public class Waypoint extends LongLat {

    Float altitude;
    Object additionalData;

}
