package gyro.core.diff;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gyro.core.scope.Settings;

public class ConfiguredFieldsSettings extends Settings {

    Map<String, Map<String, Set<String>>> storedConfiguredFields = new HashMap<>();

    public Map<String, Map<String, Set<String>>> getStoredConfiguredFields() {
        return storedConfiguredFields;
    }

    public void setStoredConfiguredFields(Map<String, Map<String, Set<String>>> storedConfiguredFields) {
        this.storedConfiguredFields = storedConfiguredFields;
    }

}
