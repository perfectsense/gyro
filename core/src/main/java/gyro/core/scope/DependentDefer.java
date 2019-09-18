package gyro.core.scope;

import java.util.List;
import java.util.stream.Stream;

import gyro.core.GyroUI;

class DependentDefer extends Defer {

    private final List<Defer> errors;

    public DependentDefer(List<Defer> errors, CreateResourceDefer cause) {
        super(null, null, cause);

        this.errors = errors;
    }

    @Override
    public Stream<Defer> stream() {
        return errors.stream().flatMap(Defer::stream);
    }

    @Override
    public void write(GyroUI ui) {
        getCause().write(ui);

        ui.indented(() -> {
            ui.write("\n@|red Related errors:|@\n");

            for (int i = 0, l = errors.size(); i < l; ++i) {
                Defer error = errors.get(i);

                ui.write("\n@|red %s.|@ ", i + 1);
                ui.indented(() -> error.write(ui));
            }
        });
    }

}
