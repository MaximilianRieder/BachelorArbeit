package busrouting.configLoading;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileExceptionLoader {
    String fileLocation;

    public FileExceptionLoader(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public FileExceptions getStoppingPointExceptions() throws FileNotFoundException {
        Yaml yaml = new Yaml(new Constructor(FileExceptions.class));
        InputStream inputStream = new FileInputStream(new File(fileLocation));
        FileExceptions fileExceptions = yaml.load(inputStream);
        return fileExceptions;
    }
}
