package gyro.core.scope;

import java.util.List;

import gyro.core.GyroUI;

class MultipleDefers extends Defer {

    private final List<Defer> errors;

    public MultipleDefers(List<Defer> errors) {
        super(null, null, null);

        this.errors = errors;
    }

    @Override
    public void write(GyroUI ui) {
        ui.write("@|red Multiple errors:|@\n");

        for (int i = 0, l = errors.size(); i < l; ++i) {
            Defer error = errors.get(i);

            ui.write("\n@|red %s.|@ ", i + 1);
            ui.indented(() -> error.write(ui));
        }
    }

}
