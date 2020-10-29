package at.taaja.redcat.repositories;

import io.taaja.AbstractRepository;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class ExtensionAsObjectRepository extends AbstractRepository<Object> {

    public ExtensionAsObjectRepository() {
        super("spatialEntity", Object.class);
    }

}
