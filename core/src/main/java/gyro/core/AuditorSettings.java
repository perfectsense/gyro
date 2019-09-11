package gyro.core;

import gyro.core.scope.Settings;

import java.util.HashMap;
import java.util.Map;

public class AuditorSettings extends Settings {
    private Map<String, Class> auditorClasses;
    private final Map<String, GyroAuditor> auditorMap = new HashMap<>();

    public Map<String, Class> getAuditorClasses() {
        if (auditorClasses == null) {
            auditorClasses = new HashMap<>();
        }
        return auditorClasses;
    }

    public void setAuditorClasses(Map<String, Class> auditorsClasses) {
        this.auditorClasses = auditorsClasses;
    }

    public Map<String, GyroAuditor> getAuditorMap() {
        return auditorMap;
    }

}
