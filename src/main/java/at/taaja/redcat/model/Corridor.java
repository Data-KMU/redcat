package at.taaja.redcat.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.List;

@Data
public class Corridor extends AbstractExtension {

    public enum ShapeType{
        Circular("circular");

        private final String value;

        ShapeType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    private ShapeType shape;
    private List<List<Waypoint>> coordinates;

}
