import Broker.KafkaClient;
import Database.Mongo;
import Utils.Settings;

import java.io.IOException;

public class Main {

    public static void main(String args[]) throws IOException {

        //init settings
        Settings.getInstance().load(args);

        System.out.println(Settings.getInstance().isDebug());


        //init db
        Mongo.getInstance().openConnection();

        KafkaClient.getInstance().openConnection();




    }

}
