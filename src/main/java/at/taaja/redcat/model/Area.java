package at.taaja.redcat.model;

import lombok.Data;

import java.util.List;

@Data
public class Area extends AbstractExtension {

    private Float elevation;
    private Float height;

    private List<List<LongLat>> coordinates;

}
