package gyro.core.resource;

import gyro.core.GyroUI;
import gyro.core.scope.State;

import java.util.Set;

public abstract class Modification<T> extends Diffable {

    public boolean refresh(T current) {
        return false;
    }

    public void create(GyroUI ui, State state) throws Exception {

    }

    public void update(GyroUI ui, State state, T current, Set<String> changedFieldNames) throws Exception {

    }

    public void delete(GyroUI ui, State state) throws Exception {

    }

}
