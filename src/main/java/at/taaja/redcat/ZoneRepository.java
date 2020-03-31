package at.taaja.redcat;

import at.taaja.redcat.model.AbstractExtension;
import at.taaja.redcat.model.Area;
import at.taaja.redcat.model.Corridor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.MongoClient;
import io.quarkus.runtime.StartupEvent;
import org.bson.Document;
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

    private JacksonMongoCollection<Corridor> corridor;
    private JacksonMongoCollection<Area> area;

    void onStart(@Observes StartupEvent ev) {
        corridor = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "corridor", Corridor.class);

       area = JacksonMongoCollection
                .builder()
                .build(this.mongoClient, this.database, "area", Area.class);
    }


//    public CompletionStage<Optional<Area>> getArea(String areaId){
//        return reactiveMongoClient
//                .getDatabase(this.database)
//                .getCollection("area", Area.class)
//                .find(Filters.eq("_id", areaId)).findFirst().run();
//
//    }

    public Area getArea(String areaId){
        return this.area.find(Filters.eq("_id", areaId)).first();
    }

    public Corridor getCorridor(String corridorId){
        return this.corridor.find(Filters.eq("_id", corridorId)).first();
    }

    public void insertExtension(AbstractExtension abstractExtension) {
        if(abstractExtension instanceof Corridor){
            JacksonMongoCollection
                    .builder()
                    .build(this.mongoClient, this.database, "corridor",  Corridor.class)
                    .insertOne((Corridor) abstractExtension);
        }else if(abstractExtension instanceof Area){
            JacksonMongoCollection
                    .builder()
                    .build(this.mongoClient, this.database, "area",  Area.class)
                    .insertOne((Area) abstractExtension);

        }

    }


}
