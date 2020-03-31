package at.taaja.redcat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.taaja.models.zoning.Extension;
import lombok.Data;
import org.mongojack.Id;

import java.util.List;

@Data
public class Area extends AbstractExtension {


    private String type;
    private Float elevation;
    private Float height;

    private List<List<LongLat>> coordinates;



}
