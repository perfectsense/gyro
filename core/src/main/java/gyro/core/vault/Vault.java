package gyro.core.vault;

import java.util.List;
import java.util.Map;

public abstract class Vault {

    private String name;

    public abstract String get(String key);

    public abstract boolean put(String key, String value);

    public abstract Map<String, String> list(String filter);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
