package gyro.core.resource;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.ParameterizedType;

public class ModificationPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        if (Modification.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Modification> modificationClass = (Class<? extends Modification>) aClass;

            // Find the resource that was modified and add modification class to it.
            ParameterizedType parameterizedType = (ParameterizedType) TypeResolver.reify(Modification.class, modificationClass);
            Class resourceClass = (Class) parameterizedType.getActualTypeArguments()[0];

            DiffableType resourceType = DiffableType.getInstance(resourceClass);
            resourceType.modify(modificationClass);
        }
    }

}
