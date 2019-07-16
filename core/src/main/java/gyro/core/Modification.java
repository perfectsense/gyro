package gyro.core;

import java.util.List;
import java.util.Set;

import gyro.core.resource.Resource;
import gyro.core.scope.State;

public abstract class Modification extends Resource {

    public abstract List<String> modifies();

    public void modify(Resource resource) {

    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create(GyroUI ui, State state) {

    }

    @Override
    public void update(GyroUI ui, State state, Resource current, Set<String> changedFieldNames) {

    }

    @Override
    public void delete(GyroUI ui, State state) {

    }

    @Override
    public String toDisplayString() {
        return null;
    }

}
