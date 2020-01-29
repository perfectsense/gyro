/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        List<Defer> otherErrors = new ArrayList<>();
        Map<String, List<Defer>> causedByFindErrors = new LinkedHashMap<>();

        for (Defer error : flattenedErrors) {

            // Find all resources that were defined but couldn't be created,
            // and store them by resource type and name key.
            if (error instanceof CreateDefer) {
                CreateDefer c = (CreateDefer) error;
                createErrors.put(c.getKey(), c);

            } else {
                otherErrors.add(error);
            }

            // Find the underlying cause.
            Defer cause = error;

            for (Defer c; (c = cause.getCause()) != null; ) {
                cause = c;
            }

            // If the underlying cause was not being able to find the resource by name,
            // store those, grouped by the resource type and name key.
            if (cause instanceof FindDefer) {
                causedByFindErrors.computeIfAbsent(((FindDefer) cause).getKey(), k -> new ArrayList<>()).add(error);
            }
        }

        // If something failed because the underlying cause was not being able to find the resource by name,
        // see if there's a corresponding error where the resource couldn't be created.
        Map<String, DependentDefer> dependentErrorById = new LinkedHashMap<>();

        for (Map.Entry<String, List<Defer>> entry : causedByFindErrors.entrySet()) {
            String k = entry.getKey();
            CreateDefer c = createErrors.get(k);

            if (c != null) {
                dependentErrorById.put(k, new DependentDefer(c, entry.getValue()));
            }
        }

        // Start constructing a list of all errors to display to the user.
        List<Defer> displayErrors = new ArrayList<>();

        displayErrors.addAll(createErrors.values());
        displayErrors.addAll(otherErrors);

        // Remove all errors associated with dependent errors to avoid duplicate display.
        Collection<DependentDefer> dependentErrors = dependentErrorById.values();

        displayErrors.addAll(dependentErrors);

        dependentErrors.forEach(e -> {
            displayErrors.remove(e.getCause());
            e.getRelated().forEach(displayErrors::remove);
        });

        // Find circular dependencies among dependent errors.
        while (!dependentErrorById.isEmpty()) {
            Iterator<DependentDefer> i = dependentErrors.iterator();
            DependentDefer d = i.next();
            Set<CreateDefer> seen = new LinkedHashSet<>();

            if (findCircularDependency(dependentErrorById, d, seen)) {

                // Once a circular dependency is found, find all related errors that it caused.
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

    // Find the circular dependency by looking at all related errors. If a create error is seen more than once,
    // we know that there's a circular dependency, and everything seen so far is involved in causing the circular
    // dependency.
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
