package gyro.core.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Diff;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.block.ResourceNode;
import gyro.util.ImmutableCollectors;

public class Stage {

    private final String name;
    private final boolean confirmDiff;
    private final String transitionPrompt;
    private final List<Create> creates;
    private final List<Delete> deletes;
    private final List<Swap> swaps;
    private final List<Transition> transitions;

    public Stage(String name, Scope scope) {
        this.name = Preconditions.checkNotNull(name, "Stage requires a name!");
        this.confirmDiff = Boolean.TRUE.equals(scope.get("confirm-diff"));
        this.transitionPrompt = (String) scope.get("transition-prompt");

        WorkflowSettings settings = scope.getSettings(WorkflowSettings.class);

        this.creates = settings.getCreates();
        this.deletes = settings.getDeletes();
        this.swaps = settings.getSwaps();

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

        Scope executeScope = new Scope(pendingRootScope.getFileScopes()
            .stream()
            .filter(s -> s.getFile().equals(pendingResource.scope().getFileScope().getFile()))
            .findFirst()
            .orElse(null));

        NodeEvaluator evaluator = executeScope.getRootScope().getEvaluator();

        executeScope.put("NAME", pendingResource.name());
        executeScope.put("CURRENT", currentResource);
        executeScope.put("PENDING", pendingResource.scope().resolve());

        for (Create create : creates) {
            evaluator.visit(
                new ResourceNode(
                    (String) evaluator.visit(create.getType(), executeScope),
                    create.getName(),
                    create.getBody()),
                executeScope);
        }

        deletes.forEach(d -> d.execute(ui, currentRootScope, pendingRootScope, executeScope));

        for (Swap swap : swaps) {
            String type = (String) evaluator.visit(swap.getType(), executeScope);
            String x = getResourceName(evaluator.visit(swap.getX(), executeScope));
            String y = getResourceName(evaluator.visit(swap.getY(), executeScope));

            ui.write("@|magenta ⤢ Swapping %s with %s|@\n", x, y);
            state.swap(currentRootScope, pendingRootScope, type, x, y);
        }

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

    private String getResourceName(Object value) {
        if (value instanceof Resource) {
            return ((Resource) value).name();

        } else if (value instanceof String) {
            return (String) value;

        } else {
            throw new GyroException(String.format(
                "Can't swap an instance of [%s]!",
                value.getClass().getName()));
        }
    }

}
