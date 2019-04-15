package gyro.core;

import java.util.List;
import java.util.Set;

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
    public void create() {

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        return null;
    }

    @Override
    public Class resourceCredentialsClass() {
        return null;
    }

}
