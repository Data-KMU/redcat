package at.taaja.redcat;

import io.taaja.AbstractRepository;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class ExtensionObjectRepository extends AbstractRepository<Object> {

    public ExtensionObjectRepository() {
        super("spatialEntity", Object.class);
    }

}
