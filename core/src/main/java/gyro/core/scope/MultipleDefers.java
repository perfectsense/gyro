package gyro.core.scope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import gyro.core.GyroUI;

class MultipleDefers extends Defer {

    private final List<Defer> errors;

    public MultipleDefers(List<Defer> errors) {
        super(null, null, null);

        this.errors = errors;
    }

    @Override
    public Stream<Defer> stream() {
        return errors.stream().flatMap(Defer::stream);
    }

    @Override
    public void write(GyroUI ui) {
        Map<String, CreateResourceDefer> createResourceErrors = new LinkedHashMap<>();
        Map<String, List<Defer>> causedByFindByNameErrors = new LinkedHashMap<>();
        List<Defer> otherErrors = new ArrayList<>();

        stream().flatMap(Defer::stream).forEach(e -> {
            if (e instanceof CreateResourceDefer) {
                CreateResourceDefer c = (CreateResourceDefer) e;
                createResourceErrors.put(c.getId(), c);
            }

            Defer cause = e;

            for (Defer c; (c = cause.getCause()) != null; ) {
                cause = c;
            }

            if (cause instanceof FindByNameDefer) {
                causedByFindByNameErrors.computeIfAbsent(
                    ((FindByNameDefer) cause).getId(),
                    k -> new ArrayList<>())
                    .add(e);

            } else if (!(e instanceof CreateResourceDefer)) {
                otherErrors.add(e);
            }
        });

        List<DependentDefer> dependentErrors = new ArrayList<>();

        for (Map.Entry<String, List<Defer>> entry : causedByFindByNameErrors.entrySet()) {
            CreateResourceDefer c = createResourceErrors.get(entry.getKey());

            if (c != null) {
                dependentErrors.add(new DependentDefer(entry.getValue(), c));
            }
        }

        List<Defer> displayErrors = new ArrayList<>();

        displayErrors.addAll(createResourceErrors.values());
        displayErrors.addAll(otherErrors);
        displayErrors.addAll(dependentErrors);

        dependentErrors.forEach(e -> {
            displayErrors.remove(e.getCause());
            e.stream().forEach(displayErrors::remove);
        });

        int displayErrorsSize = displayErrors.size();

        if (displayErrorsSize == 0) {
            return;
        }

        displayErrors.get(0).write(ui);

        displayErrors.subList(1, displayErrorsSize).forEach(e -> {
            ui.write("\n@|red ---|@\n\n");
            e.write(ui);
        });
    }

}
