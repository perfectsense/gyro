package beam.lang.ast;

import java.util.LinkedHashMap;
import java.util.Map;

public class Resource {

    private final String type;
    private final String name;
    private final Map<String, Object> values;

    public Resource(String type, String name, Scope scope) {
        this.type = type;
        this.name = name;

        // State should probably be loaded here.
        this.values = scope != null ? new LinkedHashMap<>(scope) : new LinkedHashMap<>();
    }

    public String getType() {
        return type;
    }

    public Object get(String key) {
        return values.getOrDefault(key, "<FETCH " + type + " " + name + " " + key + ">");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(type);
        builder.append(' ');
        builder.append(name);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            builder.append("\n    ");
            builder.append(entry.getKey());
            builder.append(": ");
            builder.append(entry.getValue());
        }

        return builder.toString();
    }
}
