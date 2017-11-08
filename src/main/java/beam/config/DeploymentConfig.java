package beam.config;

import beam.BeamCloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigKey("type")
public abstract class DeploymentConfig extends Config {
    private String type;
    private Map<String, Object> extraAttributes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getExtraAttributes() {
        if (extraAttributes == null) {
            extraAttributes = new HashMap<>();
        }

        return extraAttributes;
    }

    public void setExtraAttributes(Map<String, Object> extraAttributes) {
        this.extraAttributes = extraAttributes;
    }

    public abstract Map<String, String> prepare(BeamCloud cloud, Object pending);

    public void afterPush() {

    }

    public void afterRevert() {

    }

    public void afterCommit() {

    }

    public abstract Map<String, String> getGroupHashItems();

    public abstract List<String> getGroupHashKeys();

    public abstract String toDisplayString();
}
