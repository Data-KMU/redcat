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

    private JacksonMongoCollection<SpatialEntity> extensionCollection;
    private JacksonMongoCollection<Object> objectCollection;

    void onStart(@Observes StartupEvent ev) {
        extensionCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "extension", SpatialEntity.class, UuidRepresentation.STANDARD);

        objectCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "extension", Object.class, UuidRepresentation.STANDARD);
    }




    public SpatialEntity getExtension(String extensionId){
        return this.extensionCollection.find(Filters.eq("_id", extensionId)).first();
    }

    public Object getExtensionAsObject(String extensionId){
        return this.objectCollection
                .find(Filters.eq("_id", extensionId)).first();
    }


    public void insertExtension(SpatialEntity abstractExtension) {
        this.extensionCollection.insertOne(abstractExtension);
    }


    public void update(String extensionId, Object updatedExtension) {
        this.objectCollection.replaceOne(
                Filters.eq("_id", extensionId),
                updatedExtension
        );
    }

    public void addExtension(Area area) {
        this.extensionCollection.insertOne(area);
    }

    public DeleteResult removeExtension(String extensionId) {
        return this.extensionCollection.removeById(extensionId);
    }
}
