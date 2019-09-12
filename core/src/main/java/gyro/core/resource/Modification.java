package gyro.core.resource;

import gyro.core.diff.ChangeProcessor;

public abstract class Modification<T> extends Diffable implements ChangeProcessor {

    public boolean refresh(T current) {
        return false;
    }

}
