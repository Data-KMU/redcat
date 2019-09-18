package io.taaja.redcat.model;


import lombok.Data;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "corridor")
public class CorridorModel extends BaseModel {

    @TextIndexed
    private String title;
}
