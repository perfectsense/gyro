package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.scope.Settings;

import java.util.HashMap;
import java.util.Map;

public class FileBackendsSettings extends Settings {

    private Map<String, Class<? extends FileBackend>> fileBackendsClasses;
    private Map<String, FileBackend> fileBackends;

    public Map<String, Class<? extends FileBackend>> getFileBackendsClasses() {
        if (fileBackendsClasses == null) {
            fileBackendsClasses = new HashMap<>();
        }
        return fileBackendsClasses;
    }

    public void setFileBackendsClasses(Map<String, Class<? extends FileBackend>> fileBackendsClasses) {
        this.fileBackendsClasses = fileBackendsClasses;
    }

    public Map<String, FileBackend> getFileBackends() {
        if (fileBackends == null) {
            fileBackends = new HashMap<>();
        }
        return fileBackends;
    }

    public void setFileBackends(Map<String, FileBackend> fileBackends) {
        this.fileBackends = fileBackends;
    }

}
