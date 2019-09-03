package Utils;

public class Settings {

    private static Settings ourInstance;

    public static Settings getInstance() {

        Settings.ourInstance = new Settings();

        return ourInstance;
    }

    private Settings() {
    }
}
