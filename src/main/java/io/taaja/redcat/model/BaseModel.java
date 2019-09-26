package io.taaja.redcat.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.*;

import java.io.Serializable;
import java.util.Calendar;

@Setter
@Getter
public abstract class BaseModel implements Serializable {
    @Id
    private String id;

    @CreatedDate
    private Calendar createdDate;

    @LastModifiedDate
    private Calendar updatedDate;

    @Version
    private Long version;

}
