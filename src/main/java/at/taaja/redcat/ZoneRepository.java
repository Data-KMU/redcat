package at.taaja.redcat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import io.quarkus.runtime.StartupEvent;
import io.taaja.models.record.spatial.Area;
import io.taaja.models.record.spatial.SpatialEntity;
import org.bson.UuidRepresentation;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mongojack.JacksonMongoCollection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;


@ApplicationScoped
public class ZoneRepository{

//    @Inject
//    public ReactiveMongoClient reactiveMongoClient;

    @Inject
    public MongoClient mongoClient;


    @ConfigProperty(name = "app.database")
    public String database;

    private JacksonMongoCollection<SpatialEntity> spatialEntityCollection;
    private JacksonMongoCollection<Object> objectCollection;

    void onStart(@Observes StartupEvent ev) {
        spatialEntityCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "spatialEntity", SpatialEntity.class, UuidRepresentation.STANDARD);

        objectCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "spatialEntity", Object.class, UuidRepresentation.STANDARD);
    }




    public SpatialEntity getSpatialEntity(String spatialEntityId){
        return this.spatialEntityCollection.find(Filters.eq("_id", spatialEntityId)).first();
    }

    public Object getSpatialEntityAsObject(String spatialEntityId){
        return this.objectCollection
                .find(Filters.eq("_id", spatialEntityId)).first();
    }


    public void insertSpatialEntity(SpatialEntity abstractSpatialEntity) {
        this.spatialEntityCollection.insertOne(abstractSpatialEntity);
    }


    public void update(String spatialEntityId, Object updatedSpatialEntity) {
        this.objectCollection.replaceOne(
                Filters.eq("_id", spatialEntityId),
                updatedSpatialEntity
        );
    }

    public void addSpatialEntity(Area area) {
        this.spatialEntityCollection.insertOne(area);
    }

    public DeleteResult removeSpatialEntity(String spatialEntityId) {
        return this.spatialEntityCollection.removeById(spatialEntityId);
    }
}
