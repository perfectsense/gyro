package gyro.core.resource;

import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;
import net.jodah.typetools.TypeResolver;

public class ModificationPlugin extends Plugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (Modification.class.isAssignableFrom(aClass)) {
            Class<? extends Modification<Diffable>> modificationClass = (Class<? extends Modification<Diffable>>) aClass;
            Class<?> diffableClass = TypeResolver.resolveRawArgument(modificationClass, Modification.class);
            DiffableType<Diffable> type = DiffableType.getInstance((Class<Diffable>) diffableClass);

            type.modify(modificationClass);
        }
    }

}
