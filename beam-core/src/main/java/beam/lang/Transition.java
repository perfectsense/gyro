package beam.lang;

import java.util.Map;

public class Transition {

    private final String stage;
    private final String name;
    private final String description;

    public Transition(Map<String, Object> map) {
        stage = (String) map.get("stage");
        name = (String) map.get("name");
        description = (String) map.get("description");
    }

    public String getStage() {
        return stage;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
