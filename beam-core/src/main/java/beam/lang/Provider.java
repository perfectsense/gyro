package beam.lang;

import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public abstract class Provider extends Plugin {

    private static Map<String, Map<String, Class>> PROVIDER_CLASS_CACHE = new HashMap<>();

    public void classLoaded(Class<?> klass) {
        if (Modifier.isAbstract(klass.getModifiers())) {
            return;
        }

        for (ResourceName name : klass.getAnnotationsByType(ResourceName.class)) {
            registerResource(klass, name.value(), name.parent());
        }
    }

    private void registerResource(Class resourceClass, String resourceName, String parentName) {
        String fullName = String.format("%s::%s", name(), resourceName);
        if (!ObjectUtils.isBlank(parentName)) {
            fullName = String.format("%s::%s::%s", name(), parentName, resourceName);
        }

        Map<String, Class> cache = PROVIDER_CLASS_CACHE.computeIfAbsent(artifact(), f -> new HashMap<>());
        if (!cache.containsKey(fullName)) {
            cache.put(fullName, resourceClass);
        }

        core().addResourceType(fullName, cache.get(fullName));
    }

}
