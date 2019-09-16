package Database;

import Interfaces.Service;
import Utils.Settings;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;

public class Mongo implements Service {

    private static final String DEFAULT_DB_NAME = "digTwin";

    private static Mongo ourInstance = new Mongo();

    public static Mongo getInstance() {
        return ourInstance;
    }

    private MongoClient mongoClient;
    private MongoDatabase digTwin;

    private Mongo() {
    }

    public void openConnection(){
        Settings settings = Settings.getInstance();

        this.mongoClient = MongoClients.create(settings.get("mongo.connection"));
        this.digTwin = mongoClient.getDatabase(DEFAULT_DB_NAME);
    }

//    public MongoCollection<Document> insert(){
////        this.digTwin.createCollection();
//    }


    @Override
    public void close() throws Exception {
        this.mongoClient.close();
    }
}
