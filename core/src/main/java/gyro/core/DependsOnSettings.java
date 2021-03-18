package gyro.core;

import java.util.HashSet;
import java.util.Set;

import gyro.core.resource.Resource;
import gyro.core.scope.Settings;

public class DependsOnSettings extends Settings {

    private final Set<Resource> dependencies = new HashSet<>();

    public Set<Resource> getDependencies() {
        return dependencies;
    }
}
