package gyro.core.scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        Map<String, DependentDefer> dependentErrorById = new LinkedHashMap<>();

        for (Map.Entry<String, List<Defer>> entry : causedByFindByNameErrors.entrySet()) {
            String k = entry.getKey();
            CreateResourceDefer c = createResourceErrors.get(k);

            if (c != null) {
                dependentErrorById.put(k, new DependentDefer(c, entry.getValue()));
            }
        }

        List<Defer> displayErrors = new ArrayList<>();

        displayErrors.addAll(createResourceErrors.values());
        displayErrors.addAll(otherErrors);

        Collection<DependentDefer> dependentErrors = dependentErrorById.values();

        displayErrors.addAll(dependentErrors);

        dependentErrors.forEach(e -> {
            displayErrors.remove(e.getCause());
            e.stream().forEach(displayErrors::remove);
        });

        while (!dependentErrorById.isEmpty()) {
            Iterator<DependentDefer> i = dependentErrors.iterator();
            DependentDefer d = i.next();
            Set<CreateResourceDefer> seen = new LinkedHashSet<>();

            if (findCircularDependency(dependentErrorById, d, seen)) {
                List<Defer> related = new ArrayList<>();

                for (Iterator<Defer> j = displayErrors.iterator(); j.hasNext(); ) {
                    Defer je = j.next();

                    if (je instanceof DependentDefer) {
                        DependentDefer jd = (DependentDefer) je;

                        if (seen.contains(jd.getCause())) {
                            j.remove();
                            jd.stream().forEach(related::add);
                        }
                    }
                }

                related.removeAll(seen);
                displayErrors.add(new CircularDefer(seen, related));
            }

            i.remove();
        }

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

    private boolean findCircularDependency(
        Map<String, DependentDefer> dependentErrors,
        DependentDefer error,
        Set<CreateResourceDefer> seen) {

        CreateResourceDefer cause = error.getCause();

        if (!seen.add(cause)) {
            return true;
        }

        return error.stream()
            .filter(CreateResourceDefer.class::isInstance)
            .map(CreateResourceDefer.class::cast)
            .map(CreateResourceDefer::getId)
            .map(dependentErrors::get)
            .filter(Objects::nonNull)
            .anyMatch(e -> findCircularDependency(dependentErrors, e, seen));
    }

}
