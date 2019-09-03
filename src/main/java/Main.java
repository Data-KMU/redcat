import Database.Mongo;
import Utils.Settings;

import java.io.IOException;

public class Main {

    public static void main(String args[]) throws IOException {

        //init settings
        Settings.getInstance().load(args);

//        Settings.getInstance().getProperties().keySet().forEach(System.out::println);

        System.out.println(Settings.getInstance().isDebug());


        //init db
        Mongo.getInstance().openConnection();




    }

}
