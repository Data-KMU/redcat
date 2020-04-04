package at.taaja.redcat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.taaja.models.spatial.info.ExtensionIdentity;
import lombok.Data;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractExtension extends ExtensionIdentity {

    private Object actuators;
    private Object sensors;
    private Object samplers;

}
