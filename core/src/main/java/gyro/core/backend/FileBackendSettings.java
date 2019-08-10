package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.scope.Settings;

import java.util.HashMap;
import java.util.Map;

public class FileBackendSettings extends Settings {

    private Map<String, Class<? extends FileBackend>> fileBackendsClasses;
    private Map<String, FileBackend> fileBackendsByName;

    public Map<String, Class<? extends FileBackend>> getFileBackendsClasses() {
        if (fileBackendsClasses == null) {
            fileBackendsClasses = new HashMap<>();
        }
        return fileBackendsClasses;
    }

    public void setFileBackendsClasses(Map<String, Class<? extends FileBackend>> fileBackendsClasses) {
        this.fileBackendsClasses = fileBackendsClasses;
    }

    public Map<String, FileBackend> getFileBackendsByName() {
        if (fileBackendsByName == null) {
            fileBackendsByName = new HashMap<>();
        }
        return fileBackendsByName;
    }

    public void setFileBackendsByName(Map<String, FileBackend> fileBackendsByName) {
        this.fileBackendsByName = fileBackendsByName;
    }

}
