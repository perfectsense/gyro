package gyro.core.reference;

import java.util.HashMap;
import java.util.Map;

import gyro.core.Reflections;
import gyro.core.scope.Settings;

public class ReferenceSettings extends Settings {

    private final Map<String, ReferenceResolver> resolvers = new HashMap<>();

    public ReferenceResolver getResolver(String type) {
        return resolvers.get(type);
    }

    public void addResolver(Class<? extends ReferenceResolver> resolverClass) {
        ReferenceResolver resolver = Reflections.newInstance(resolverClass);
        resolvers.put(resolver.getName(), resolver);
    }

}
