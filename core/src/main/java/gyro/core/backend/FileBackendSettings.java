package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.scope.Settings;

import java.util.HashMap;
import java.util.Map;

public class FileBackendSettings extends Settings {

    private Map<String, Class<? extends FileBackend>> fileBackendClasses;
    private Map<String, FileBackend> fileBackendByName;
    private String useCredentials;

    public Map<String, Class<? extends FileBackend>> getFileBackendClasses() {

        if(fileBackendClasses == null) {
            fileBackendClasses = new HashMap<>();
        }
        return fileBackendClasses;
    }

    public void setFileBackendClasses(Map<String, Class<? extends FileBackend>> fileBackendClasses) {
        this.fileBackendClasses = fileBackendClasses;
    }

    public Map<String, FileBackend> getFileBackendByName() {
        if (fileBackendByName == null) {
            fileBackendByName = new HashMap<>();
        }

        return fileBackendByName;
    }

    public void setFileBackendByName(Map<String, FileBackend> fileBackendByName) {
        this.fileBackendByName = fileBackendByName;
    }

    public String getUseCredentials() {
        return useCredentials;
    }

    public void setUseCredentials(String useCredentials) {
        this.useCredentials = useCredentials;
    }

    public Class<? extends FileBackend> get(String local) {
        return null;
    }
}
