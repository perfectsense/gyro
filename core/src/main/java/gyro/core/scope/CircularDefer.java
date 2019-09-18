package gyro.core.scope;

import java.util.List;
import java.util.Set;

import gyro.core.GyroUI;

class CircularDefer extends Defer {

    private final Set<CreateResourceDefer> errors;
    private final List<Defer> related;

    public CircularDefer(Set<CreateResourceDefer> errors, List<Defer> related) {
        super(null, null, null);

        this.errors = errors;
        this.related = related;
    }

    @Override
    public void write(GyroUI ui) {
        writeErrors(ui, "@|red Circular dependency detected!|@\n", errors);
        writeErrors(ui, "\n@|red Related:|@\n", related);
    }

}
