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

import gyro.core.GyroUI;

class ExecuteDefer extends Defer {

    private final List<Defer> errors;

    public ExecuteDefer(List<Defer> errors) {
        super(null, null, null);

        this.errors = errors;
    }

    @Override
    public void write(GyroUI ui) {
        List<Defer> flattenedErrors = new ArrayList<>();

        flattenErrors(errors, flattenedErrors);

        Map<String, CreateDefer> createErrors = new LinkedHashMap<>();
        Map<String, List<Defer>> causedByFindByNameErrors = new LinkedHashMap<>();
        List<Defer> otherErrors = new ArrayList<>();

        for (Defer error : flattenedErrors) {
            if (error instanceof CreateDefer) {
                CreateDefer c = (CreateDefer) error;
                createErrors.put(c.getKey(), c);
            }

            Defer cause = error;

            for (Defer c; (c = cause.getCause()) != null; ) {
                cause = c;
            }

            if (cause instanceof FindByNameDefer) {
                causedByFindByNameErrors.computeIfAbsent(
                    ((FindByNameDefer) cause).getKey(),
                    k -> new ArrayList<>())
                    .add(error);

            } else if (!(error instanceof CreateDefer)) {
                otherErrors.add(error);
            }
        }

        Map<String, DependentDefer> dependentErrorById = new LinkedHashMap<>();

        for (Map.Entry<String, List<Defer>> entry : causedByFindByNameErrors.entrySet()) {
            String k = entry.getKey();
            CreateDefer c = createErrors.get(k);

            if (c != null) {
                dependentErrorById.put(k, new DependentDefer(c, entry.getValue()));
            }
        }

        List<Defer> displayErrors = new ArrayList<>();

        displayErrors.addAll(createErrors.values());
        displayErrors.addAll(otherErrors);

        Collection<DependentDefer> dependentErrors = dependentErrorById.values();

        displayErrors.addAll(dependentErrors);

        dependentErrors.forEach(e -> {
            displayErrors.remove(e.getCause());
            e.getRelated().forEach(displayErrors::remove);
        });

        while (!dependentErrorById.isEmpty()) {
            Iterator<DependentDefer> i = dependentErrors.iterator();
            DependentDefer d = i.next();
            Set<CreateDefer> seen = new LinkedHashSet<>();

            if (findCircularDependency(dependentErrorById, d, seen)) {
                List<Defer> related = new ArrayList<>();

                for (Iterator<Defer> j = displayErrors.iterator(); j.hasNext(); ) {
                    Defer je = j.next();

                    if (je instanceof DependentDefer) {
                        DependentDefer jd = (DependentDefer) je;

                        if (seen.contains(jd.getCause())) {
                            j.remove();
                            related.addAll(jd.getRelated());
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

    private void flattenErrors(List<Defer> source, List<Defer> target) {
        for (Defer error : source) {
            if (error instanceof ExecuteDefer) {
                flattenErrors(((ExecuteDefer) error).errors, target);

            } else {
                target.add(error);
            }
        }
    }

    private boolean findCircularDependency(
        Map<String, DependentDefer> dependentErrors,
        DependentDefer error,
        Set<CreateDefer> seen) {

        CreateDefer cause = error.getCause();

        if (!seen.add(cause)) {
            return true;
        }

        return error.getRelated()
            .stream()
            .filter(CreateDefer.class::isInstance)
            .map(CreateDefer.class::cast)
            .map(CreateDefer::getKey)
            .map(dependentErrors::get)
            .filter(Objects::nonNull)
            .anyMatch(e -> findCircularDependency(dependentErrors, e, seen));
    }

}
