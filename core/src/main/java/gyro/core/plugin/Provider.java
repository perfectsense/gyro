package gyro.core.plugin;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.ResourceFinder;
import gyro.core.resource.ResourceType;

public abstract class Provider extends Plugin {

    private static Map<String, Map<String, Class>> PROVIDER_CLASS_CACHE = new HashMap<>();

    public void classLoaded(Class<?> klass) {
        if (Modifier.isAbstract(klass.getModifiers())) {
            return;
        }

        for (ResourceType type : klass.getAnnotationsByType(ResourceType.class)) {
            if (ResourceFinder.class.isAssignableFrom(klass)) {
                registerResourceFinder(klass, type.value());
            } else {
                registerResource(klass, type.value());
            }
        }
    }

    private void registerResource(Class resourceClass, String type) {
        String fullName = String.format("%s::%s", name(), type);

        Map<String, Class> cache = PROVIDER_CLASS_CACHE.computeIfAbsent(artifact(), f -> new HashMap<>());
        if (!cache.containsKey(fullName)) {
            cache.put(fullName, resourceClass);
        }

        getScope().getRootScope().getResourceClasses().put(fullName, cache.get(fullName));
    }

    private void registerResourceFinder(Class queryClass, String type) {
        String fullName = String.format("%s::%s", name(), type);

        String registerName = String.format("%s::%s", fullName, "_query");
        Map<String, Class> cache = PROVIDER_CLASS_CACHE.computeIfAbsent(artifact(), f -> new HashMap<>());
        if (!cache.containsKey(registerName)) {
            cache.put(registerName, queryClass);
        }

        getScope().getRootScope().getResourceFinderClasses().put(fullName, cache.get(registerName));
    }
}
