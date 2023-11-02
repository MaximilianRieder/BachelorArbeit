package busrouting.configLoading;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ConfigDayDataLoader {
    String fileLocation;

    public ConfigDayDataLoader(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public ConfigDayData getConfigDayData () throws FileNotFoundException {
        Yaml yaml = new Yaml(new Constructor(ConfigDayData.class));
        InputStream inputStream = new FileInputStream(new File(fileLocation));
        ConfigDayData configDayData = yaml.load(inputStream);
        return configDayData;
    }
}
