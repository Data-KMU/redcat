package at.taaja.redcat.model;

import io.taaja.models.spatial.ExtensionType;
import lombok.Data;

import java.util.List;

@Data
public class Area extends AbstractExtension {

    private Float elevation;
    private Float height;

    private List<List<LongLat>> coordinates;

    public Area(){
        this.setType(ExtensionType.Area);
    }

}
