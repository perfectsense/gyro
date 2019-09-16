package gyro.core.resource;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import net.jodah.typetools.TypeResolver;

public class ModificationPlugin extends Plugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Modification.class.isAssignableFrom(aClass)) {
            Class<? extends Modification> modificationClass = (Class<? extends Modification>) aClass;
            Class<?> diffableClass = TypeResolver.resolveRawArgument(modificationClass, Modification.class);
            DiffableType type = DiffableType.getInstance((Class<? extends Diffable>) diffableClass);

            type.modify(modificationClass);
        }
    }

}
