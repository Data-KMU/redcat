package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Settings {

    private final static Logger LOGGER = Logger.getLogger(Settings.class.getName());

    public static final String DEFAULT_PROPERTY_FILE_NAME = "default.properties";

    private static Settings ourInstance = new Settings();

    public static Settings getInstance() {
        return ourInstance;
    }

    private Properties propertyFile = new Properties();
    private Properties commandLineArguments = new Properties(propertyFile);

    private Settings() {
    }

    public void load(String[] arguments) throws IOException {

        for (String argument : arguments) {
            int index = argument.indexOf('=');
            this.commandLineArguments.setProperty(argument.substring(0, index), argument.substring(index + 1));
        }

        String propertyFileName = this.commandLineArguments.getProperty("profile", DEFAULT_PROPERTY_FILE_NAME);

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("Properties/" + propertyFileName)) {
            propertyFile.load(input);
        }

    }

    public Properties getProperties(){
        return this.commandLineArguments;
    }

    public boolean isDebug(){
        return Boolean.valueOf(this.getProperties().getProperty("app.debug"));
    }

}
