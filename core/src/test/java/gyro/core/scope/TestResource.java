package gyro.core.scope;

import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.Namespace;
import gyro.core.Type;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;

@Namespace("test")
@Type("resource")
public class TestResource extends Resource {

    public TestResource(String name) {
        DiffableInternals.setName(this, name);
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
