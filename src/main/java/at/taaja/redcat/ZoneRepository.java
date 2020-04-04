package at.taaja.redcat;

import at.taaja.redcat.model.AbstractExtension;
import at.taaja.redcat.model.Area;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import io.quarkus.runtime.StartupEvent;
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

    private JacksonMongoCollection<AbstractExtension> extensionCollection;
    private JacksonMongoCollection<Object> objectCollection;

    void onStart(@Observes StartupEvent ev) {
        extensionCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "extension", AbstractExtension.class);

        objectCollection = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "extension", Object.class);
    }




    public AbstractExtension getExtension(String extensionId){
        return this.extensionCollection.find(Filters.eq("_id", extensionId)).first();
    }

    public Object getExtensionAsObject(String extensionId){
        return this.objectCollection
                .find(Filters.eq("_id", extensionId)).first();
    }


    public void insertExtension(AbstractExtension abstractExtension) {
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
}
