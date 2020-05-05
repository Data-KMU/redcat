package at.taaja.redcat;

import io.taaja.AbstractRepository;
import io.taaja.models.record.spatial.SpatialEntity;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class ExtensionRepository extends AbstractRepository<SpatialEntity> {

    public ExtensionRepository() {
        super("spatialEntity", SpatialEntity.class);
    }

}
