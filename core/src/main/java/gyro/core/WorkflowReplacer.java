/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diff;
import gyro.core.diff.Retry;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.core.workflow.ModifiedIn;
import gyro.core.workflow.ReplaceResource;
import gyro.core.workflow.Workflow;

public abstract class WorkflowReplacer {

    public static final String EXECUTION_FILE = "workflow-replacer-execution.json";
    private static final List<WorkflowReplacer> SUCCESSFULLY_EXECUTED_WORKFLOW_REPLACERS = new ArrayList<>();

    private final List<String> toBeRemoved = new ArrayList<>();
    private final List<ReplaceResource> toBeReplaced = new ArrayList<>();
    private final List<Stage> executedStages = new ArrayList<>();
    private final Map<String, Map<String, ModifiedIn>> modifiedInFileScopeMap = new HashMap<>();

    private Scope scope;

    public List<Stage> getExecutedStages() {
        return executedStages;
    }

    public List<String> getToBeRemoved() {
        return toBeRemoved;
    }

    public List<ReplaceResource> getToBeReplaced() {
        return toBeReplaced;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getExecution(RootScope root) {
        try (GyroInputStream input = root.openInput(EXECUTION_FILE)) {
            return (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(input, StandardCharsets.UTF_8));

        } catch (GyroException error) {
            return null;

        } catch (IOException error) {
            throw new GyroException(error);
        }
    }

    public Stage getStage(String name, List<Stage> stages) {
        return stages.stream()
            .filter(s -> name.equals(s.getName()))
            .findAny()
            .orElseThrow(() -> new GyroException(String.format(
                "Can't find @|bold %s|@ stage in @|bold %s|@ workflow!",
                name,
                label())));
    }

    @SuppressWarnings("unchecked")
    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource) {
        List<Stage> stages = getStages(ui, state, currentResource, pendingResource);

        RootScope root = scope.getRootScope();

        Map<String, Object> execution = getExecution(root.getCurrent());
        Stage stage;

        if (execution != null) {
            ((List<String>) execution.get("executedStages")).stream()
                .map(s -> getStage(s, stages))
                .collect(Collectors.toCollection(() -> executedStages));
            stage = executedStages.get(executedStages.size() - 1);
            ui.write("\n@|magenta · Resuming from %s stage|@\n", stage.getName());
        } else {
            stage = stages.iterator().next();
        }

        // TODO: optimize performance.
        while (stage != null) {
            ui.write("\n@|magenta · Executing %s stage|@\n", stage.getName());

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            int indexOfCurrentStage = executedStages.indexOf(stage);

            if (indexOfCurrentStage > -1) {
                ListIterator<Stage> iterator = executedStages.listIterator(indexOfCurrentStage + 1);

                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            } else {
                executedStages.add(stage);
            }

            RootScope pending = copyRootScope(root);

            for (Stage executedStage : executedStages) {
                executedStage.getApply().run();
            }

            ui.indent();

            try {
                executeChanges(ui, stage, currentResource, state);
                stage = transition(ui, state, pending.getCurrent(), stage);

                if (stage != null) {
                    backupModifiedInValues(pending);
                }
            } finally {
                ui.unindent();
            }
        }
        // TODO: USE IN METADATA
        SUCCESSFULLY_EXECUTED_WORKFLOW_REPLACERS.add(this);

        throw Retry.INSTANCE;
    }

    private void backupModifiedInValues(RootScope pending) {
        modifiedInFileScopeMap.clear();

        for (FileScope fileScope : pending.getFileScopes()) {
            Map<String, ModifiedIn> modifiedInMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : fileScope.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Resource) {
                    ModifiedIn modifiedIn = DiffableInternals.getModifiedIn((Diffable) value);

                    if (modifiedIn != null) {
                        modifiedInMap.put(entry.getKey(), modifiedIn);
                    }
                }
            }

            if (!modifiedInMap.isEmpty()) {
                modifiedInFileScopeMap.put(fileScope.getFile(), modifiedInMap);
            }
        }
    }

    private void restoreModifiedInValues(RootScope pending) {
        for (FileScope fileScope : pending.getFileScopes()) {
            Map<String, ModifiedIn> modifiedInMap = modifiedInFileScopeMap.get(fileScope.getFile());

            if (modifiedInMap != null) {
                for (Map.Entry<String, ModifiedIn> entry : modifiedInMap.entrySet()) {
                    Object resource = fileScope.get(entry.getKey());

                    if (resource instanceof Resource) {
                        DiffableInternals.setModifiedIn((Diffable) resource, entry.getValue());
                    }
                }
            }
        }
    }

    private RootScope copyRootScope(RootScope root) {
        RootScope current = root.getCurrent();
        current.setWorkflow();
        current.getFileScopes().clear();
        current.evaluate();

        RootScope pending = new RootScope(
            root.getFile(),
            root.getBackend(),
            null,
            current,
            root.getLoadFiles(),
            true);
        pending.evaluate();

        restoreModifiedInValues(pending);

        return pending;
    }

    private Stage transition(GyroUI ui, State state, RootScope currentRootScope, Stage stage) {
        List<Stage> transitions = stage.getTransitions();

        if (transitions == null || transitions.size() == 0) {
            currentRootScope.delete(Workflow.EXECUTION_FILE);
            state.setRemoveModifiedInField(true);
            state.save();
            return null;
        } else if (transitions.size() == 1) {
            Stage selectedStage = transitions.get(0);
            ui.write("\n@|magenta · Executing %s stage|@\n", selectedStage.getName());
            return selectedStage;
        }

        while (true) {
            for (Stage stageTransition : transitions) {
                ui.write("\n%s) %s", stageTransition.getName(), stageTransition.getDescription());
            }

            String selected = ui.readText("\nNext stage? ");

            Stage selectedStage = transitions.stream()
                .filter(t -> selected.equals(t.getName()))
                .findFirst()
                .orElse(null);

            if (selectedStage != null) {
                return selectedStage;
            } else {
                ui.write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

    private static void extend(Resource source, Set<String> excludes, Scope scope) {
        Map<String, Object> sourceMap;

        if (source == null) {
            throw new GyroException("Can't extend from a null!");
        }

        sourceMap = DiffableInternals.getScope(source);

        if (sourceMap == null) {
            sourceMap = DiffableType.getInstance(source)
                .getFields()
                .stream()
                .collect(
                    LinkedHashMap::new,
                    (m, f) -> m.put(f.getName(), f.getValue(source)),
                    LinkedHashMap::putAll);
        }

        sourceMap = sourceMap.entrySet()
            .stream()
            .filter(e -> !excludes.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        sourceMap.forEach(scope::putIfAbsent);
    }

    protected Resource extendResource(Resource resource, String newName) {
        DiffableScope diffableScope = new DiffableScope(DiffableInternals.getScope(resource).getParent(), null);
        extend(resource, new HashSet<>(), diffableScope);

        return DiffableType.getInstance(resource).newInternal(diffableScope, newName);
    }

    protected Resource findResource(String resourceName) {
        return scope.getRootScope().findResource(resourceName);
    }

    protected Resource findCurrentResource(String resourceName) {
        return scope.getRootScope().getCurrent().findResource(resourceName);
    }

    protected void setField(Resource resource, String fieldName, Object value) {
        //TODO: Enable _configured-fields tracking
    }

    protected void queueCreate(Resource resource) {
        FileScope pending = scope.getFileScope();

        pending.put(
            DiffableType.getInstance(resource).getName() + "::" + DiffableInternals.getName(resource),
            resource);

        DiffableInternals.setModifiedIn(resource, ModifiedIn.WORKFLOW_ONLY);
    }

    protected void queueDelete(Resource pendingResource) {
        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();
        Resource currentResource = current.findResource(pendingResource.primaryKey());
        ModifiedIn modifiedIn = null;

        try {
            modifiedIn = DiffableInternals.getModifiedIn(currentResource) == ModifiedIn.WORKFLOW_ONLY
                ? ModifiedIn.WORKFLOW_ONLY
                : ModifiedIn.BOTH;
            DiffableInternals.setModifiedIn(currentResource, modifiedIn);
            current.getWorkflowRemovedResources().putIfAbsent(currentResource.primaryKey(), currentResource);
        } catch (Defer e) {
            // This is to support recovery of delete action.
        }

        DiffableInternals.setModifiedIn(pendingResource, modifiedIn == null ? ModifiedIn.WORKFLOW_ONLY : modifiedIn);
        toBeRemoved.add(pendingResource.primaryKey());
    }

    protected void queueUpdate(Resource pendingResource) {
        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();

        Resource currentResource = current.findResource(pendingResource.primaryKey());
        ModifiedIn modifiedIn = DiffableInternals.getModifiedIn(currentResource) == ModifiedIn.WORKFLOW_ONLY
            ? ModifiedIn.WORKFLOW_ONLY
            : ModifiedIn.BOTH;
        DiffableInternals.setModifiedIn(currentResource, modifiedIn);

        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);

        DiffableInternals.disconnect(pendingResource);
        DiffableInternals.update(pendingResource);
    }

    protected void queueReplace(Resource pendingResource, Resource pendingWith) {
        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();

        Resource currentResource = current.findResource(pendingResource.primaryKey());

        String pendingResourceKey = pendingResource.primaryKey();
        String currentResourceKey = currentResource.primaryKey();

        // if (current.getWorkflowReplacedResources().containsKey(currentResourceKey)) {
        //     DiffableInternals.setModifiedIn(currentResource, null);
        //     toBeRemoved.add(pendingResourceKey);
        //     toBeRemoved.add(pendingWith.primaryKey());
        //     return;
        // }
        //
        ModifiedIn modifiedIn = DiffableInternals.getModifiedIn(currentResource) == ModifiedIn.WORKFLOW_ONLY
            ? ModifiedIn.WORKFLOW_ONLY
            : ModifiedIn.BOTH;
        current.getWorkflowReplacedResources().putIfAbsent(currentResourceKey, currentResource);

        // current.getWorkflowRemovedResources().putIfAbsent(currentWith.primaryKey(), currentWith);
        // current.getWorkflowRemovedResources().putIfAbsent(currentResourceKey, currentResource);

        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);

        DiffableInternals.setModifiedIn(pendingWith, modifiedIn);

        DiffableInternals.setModifiedIn(currentResource, modifiedIn);

        toBeReplaced.add(new ReplaceResource(pendingResource, pendingWith));
        toBeRemoved.add(pendingResourceKey);
    }

    private void executeChanges(GyroUI ui, Stage stage, Resource currentResource, State state) {
        RootScope pending = scope.getRootScope();
        RootScope newPendingRootScope = pending.copyWorkflowOnlyRootScope();
        RootScope newCurrentRootScope = newPendingRootScope.getCurrent();

        Diff diff = new Diff(
            newCurrentRootScope.findSortedResourcesIn(newCurrentRootScope.getLoadFiles()),
            newPendingRootScope.findSortedResourcesIn(newPendingRootScope.getLoadFiles()),
            toBeRemoved,
            toBeReplaced);

        diff.diff();

        if (stage.isConfirmDiff() && diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", stage.getName())) {
                ui.write("\n");

            } else {
                throw new Abort();
            }
        }

        // TODO: FIX FILE
        // try (GyroOutputStream output = newCurrentRootScope.openOutput(EXECUTION_FILE)) {
        //     output.write(ObjectUtils.toJson(ImmutableMap.of(
        //         "type", DiffableType.getInstance(currentResource).getName(),
        //         "name", DiffableInternals.getName(currentResource),
        //         "replacer", label(),
        //         "executedStages", getExecutedStages()
        //     )).getBytes(StandardCharsets.UTF_8));
        // }

        diff.execute(ui, state);
    }

    public abstract List<Stage> getStages(GyroUI ui, State state, Resource currentResource, Resource pendingResource);

    public String label() {
        return Reflections.getType(getClass());
    }

    public static class Stage {

        private final String name;
        private final String description;
        private final boolean confirmDiff;
        private final Runnable apply;
        private final List<Stage> transitions;

        public Stage(String name, String description, boolean confirmDiff, Runnable apply) {
            this.name = name;
            this.description = description;
            this.confirmDiff = confirmDiff;
            this.apply = apply;
            this.transitions = new ArrayList<>();
        }

        public Stage(
            String name,
            String description,
            boolean confirmDiff,
            Runnable apply,
            List<Stage> transitions) {
            this.name = name;
            this.description = description;
            this.confirmDiff = confirmDiff;
            this.apply = apply;
            this.transitions = transitions;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isConfirmDiff() {
            return confirmDiff;
        }

        public Runnable getApply() {
            return apply;
        }

        public List<Stage> getTransitions() {
            if (transitions == null) {
                return new ArrayList<>();
            }
            return transitions;
        }
    }
}
