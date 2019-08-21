package gyro.core.virtual;

import java.util.Set;

import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;

public class VirtualRootScope extends RootScope {

    private final String virtualName;

    public VirtualRootScope(RootScope scope, String virtualName) {
        super(scope.getFile(), scope.getBackend(), null, scope.getLoadFiles());
        this.virtualName = virtualName;
        putAll(scope);
        getFileScopes().addAll(scope.getFileScopes());
    }

    @Override
    public Resource findResource(String fullName) {
        String[] names = fullName.split("::");
        names[names.length - 1] = virtualName + "/" + names[names.length - 1];
        return super.findResource(String.join("::", names));
    }

    @Override
    public void evaluate() {
        throw new UnsupportedOperationException();
    }

}
