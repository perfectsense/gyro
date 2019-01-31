package beam.lang.ast.scope;

import java.util.HashMap;
import java.util.Map;

public class RootScope extends FileScope {

    private final Map<String, Class<?>> typeClasses = new HashMap<>();

    public RootScope(String file) {
        super(null, file);
    }

    public Map<String, Class<?>> getTypeClasses() {
        return typeClasses;
    }

}
