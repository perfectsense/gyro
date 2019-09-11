package gyro.core.resource;

import java.util.Set;

public abstract class DiffableProcessor {

    public abstract Set<String> process(Diffable diffable) throws Exception;

}
