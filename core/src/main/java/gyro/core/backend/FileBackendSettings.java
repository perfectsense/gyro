package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.LocalFileBackend;
import gyro.core.scope.Settings;

import java.util.HashMap;
import java.util.Map;

public class FileBackendSettings extends Settings {

    private Map<String, Class<? extends FileBackend>> fileBackendClasses = new HashMap<>();
    private Map<String, FileBackend> fileBackendByName = new HashMap<>();

    public Map<String, Class<? extends FileBackend>> getFileBackendClasses() {
        return fileBackendClasses;
    }

    public void setFileBackendClasses(Map<String, Class<? extends FileBackend>> fileBackendClasses) {
        this.fileBackendClasses = fileBackendClasses;
    }

    public FileBackend getFileBackendByName(String name) {
        FileBackend backend = fileBackendByName.get(name);
        if (backend == null) {
            backend = fileBackendByName.get("default");
        }

        return backend;
    }

    public void putFileBackendByName(String name, FileBackend backend) {
        fileBackendByName.put(name, backend);
    }

    public void setFileBackendByName(Map<String, FileBackend> fileBackendByName) {
        this.fileBackendByName = fileBackendByName;
    }

}
