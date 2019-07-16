package gyro.core.reference;

import java.util.HashMap;
import java.util.Map;

import gyro.core.scope.Settings;

public class ReferenceSettings extends Settings {

    private Map<String, ReferenceResolver> resolvers;

    public Map<String, ReferenceResolver> getResolvers() {
        if (resolvers == null) {
            resolvers = new HashMap<>();
        }

        return resolvers;
    }

    public void setResolvers(Map<String, ReferenceResolver> resolvers) {
        this.resolvers = resolvers;
    }

}
