package gyro.core.virtual;

import com.google.common.collect.ImmutableSet;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;

import java.util.stream.Stream;

public class VirtualRootScope extends RootScope {

    private final String prefix;

    public VirtualRootScope(RootScope scope, String prefix) {
        super(scope.getFile(), scope.getBackend(), null, ImmutableSet.of());
        this.prefix = prefix;
        putAll(scope);
        getFileScopes().addAll(scope.getFileScopes());
    }

    @Override
    public Resource findResource(String fullName) {
        String[] names = fullName.split("::");
        names[names.length - 1] = prefix + "/" + names[names.length - 1];

        return Stream.concat(Stream.of(this), getFileScopes().stream())
            .map(s -> s.get(String.join("::", names)))
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void load() {
        throw new UnsupportedOperationException();
    }
}
