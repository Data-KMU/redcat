package Database;

import Utils.Settings;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;

import java.util.Properties;

public class Mongo implements AutoCloseable {

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
        Properties properties = Settings.getInstance().getProperties();

        this.mongoClient = MongoClients.create(properties.getProperty("mongo.connection"));
        this.digTwin = mongoClient.getDatabase(DEFAULT_DB_NAME);
    }

    public MongoCollection<Document> insert(){
        this.digTwin.createCollection();
    }


    @Override
    public void close() throws Exception {
        this.mongoClient.close();
    }
}
