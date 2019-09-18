package gyro.core.scope;

import java.util.List;
import java.util.stream.Stream;

import gyro.core.GyroUI;

class DependentDefer extends Defer {

    private final List<Defer> related;

    public DependentDefer(CreateResourceDefer cause, List<Defer> related) {
        super(null, null, cause);

        this.related = related;
    }

    @Override
    public Stream<Defer> stream() {
        return related.stream().flatMap(Defer::stream);
    }

    @Override
    public void write(GyroUI ui) {
        getCause().write(ui);
        writeErrors(ui, "\n@|red Related:|@\n", related);
    }

    @Override
    public CreateResourceDefer getCause() {
        return (CreateResourceDefer) super.getCause();
    }

}
