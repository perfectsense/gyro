package beam.pkg;

import io.airlift.command.Command;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Command(name = "init")
public class BasePackagePlugin {

    private Map<String, String> scope = new HashMap<>();

    public void put(String key, String value) {
        scope.put(key, value);
    }

    public String get(String key) {
        return scope.get(key);
    }

    public Set<String> keys() {
        return scope.keySet();
    }

    public void beforeProcessing(Path packagePath) {

    }

    public void afterProcessing(Path packagePath) {

    }

    public String processDirectoryName(String directoryName) {
        return directoryName;
    }

    public String processFilename(String templateFilename) {
        return templateFilename;
    }

    public String processTemplate(String templateFilename, String template) {
        for (String variable : keys()) {
            template = template.replaceAll("\\$\\{" + variable + "\\}", scope.get(variable));
        }

        return template;
    }

}