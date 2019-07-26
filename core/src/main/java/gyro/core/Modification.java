package gyro.core;

import java.util.List;
import java.util.Set;

import gyro.core.diff.Context;
import gyro.core.resource.Resource;

public abstract class Modification extends Resource {

    public abstract List<String> modifies();

    public void modify(Resource resource) {

    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create(GyroUI ui, Context context) {

    }

    @Override
    public void update(GyroUI ui, Context context, Resource current, Set<String> changedFieldNames) {

    }

    @Override
    public void delete(GyroUI ui, Context context) {

    }

}
