package at.taaja.redcat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.mongojack.Id;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Area.class, name = "Area"),
        @JsonSubTypes.Type(value = Corridor.class, name = "Corridor")
})
public abstract class AbstractExtension {

    @Id
    @JsonProperty("_id")
    private String id;

    private Object actuators;
    private Object sensors;
    private Object samplers;
}
