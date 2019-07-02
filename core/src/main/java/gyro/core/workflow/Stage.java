package gyro.core.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.DeferError;
import gyro.core.resource.Diff;
import gyro.core.resource.FileScope;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.util.ImmutableCollectors;

public class Stage {

    private final String name;
    private final boolean confirmDiff;
    private final String transitionPrompt;
    private final List<Action> actions;
    private final List<Transition> transitions;

    public Stage(String name, Scope scope) {
        this.name = Preconditions.checkNotNull(name, "Stage requires a name!");
        this.confirmDiff = Boolean.TRUE.equals(scope.get("confirm-diff"));
        this.transitionPrompt = (String) scope.get("transition-prompt");
        this.actions = ImmutableList.copyOf(scope.getSettings(WorkflowSettings.class).getActions());

        @SuppressWarnings("unchecked")
        List<Scope> transitionScopes = (List<Scope>) scope.get("transition");

        if (transitionScopes != null) {
            this.transitions = transitionScopes.stream()
                .map(s -> new Transition(scope.getName(s), s))
                .collect(ImmutableCollectors.toList());

        } else {
            this.transitions = ImmutableList.of();
        }
    }

    public String getName() {
        return name;
    }

    public String execute(
            GyroUI ui,
            State state,
            Resource currentResource,
            Resource pendingResource,
            RootScope currentRootScope,
            RootScope pendingRootScope)
            throws Exception {

        FileScope pendingFileScope = pendingResource.scope().getFileScope();
        Scope scope = new Scope(pendingRootScope.getFileScopes()
            .stream()
            .filter(s -> s.getFile().equals(pendingFileScope.getFile()))
            .findFirst()
            .orElse(null));

        for (Map.Entry<String, Object> entry : pendingFileScope.entrySet()) {
            Object value = entry.getValue();

            if (!(value instanceof Resource)) {
                scope.put(entry.getKey(), value);
            }
        }

        scope.put("NAME", pendingResource.name());
        scope.put("CURRENT", currentResource);
        scope.put("PENDING", pendingResource.scope().resolve());

        DeferError.execute(actions, a -> a.execute(ui, state, pendingRootScope, scope));

        Set<String> diffFiles = state.getDiffFiles();

        Diff diff = new Diff(
            currentRootScope.findResourcesIn(diffFiles),
            pendingRootScope.findResourcesIn(diffFiles));

        diff.diff();

        if (confirmDiff && diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", name)) {
                ui.write("\n");

            } else {
                throw new GyroException("Aborted!");
            }
        }

        diff.executeCreateOrUpdate(ui, state);
        diff.executeReplace(ui, state);
        diff.executeDelete(ui, state);

        if (transitions.isEmpty()) {
            return null;
        }

        Map<String, String> options = transitions.stream()
                .collect(Collectors.toMap(Transition::getName, Transition::getTo));

        while (true) {
            for (Transition transition : transitions) {
                ui.write("\n%s) %s", transition.getName(), transition.getDescription());
            }

            String selected = ui.readText("\n%s ", transitionPrompt != null ? transitionPrompt : "Next stage?");
            String selectedOption = options.get(selected);

            if (selectedOption != null) {
                return selectedOption;

            } else {
                ui.write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

}
