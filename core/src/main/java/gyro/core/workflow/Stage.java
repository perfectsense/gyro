package gyro.core.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.Diff;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.ValueNode;

public class Stage {

    private final String name;
    private final boolean confirmDiff;
    private final String transitionPrompt;
    private final List<DirectiveNode> creates = new ArrayList<>();
    private final List<DirectiveNode> deletes = new ArrayList<>();
    private final List<DirectiveNode> swaps = new ArrayList<>();
    private final List<Transition> transitions = new ArrayList<>();

    public Stage(Scope parent, ResourceNode node) {
        Scope scope = new Scope(parent);
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        for (Node item : node.getBody()) {
            if (item instanceof DirectiveNode) {
                DirectiveNode directive = (DirectiveNode) item;
                List<Node> arguments = directive.getArguments();

                switch (directive.getName()) {
                    case "create" :
                        if (arguments.size() != 2) {
                            throw new GyroException("@create directive only takes 2 arguments!");
                        }

                        if (directive.getBody().isEmpty()) {
                            throw new GyroException("@create directive requires a body!");
                        }

                        creates.add(directive);
                        continue;

                    case "delete" :
                        if (arguments.size() != 2) {
                            throw new GyroException("@delete directive only takes 2 arguments!");
                        }

                        deletes.add(directive);
                        continue;

                    case "swap" :
                        if (arguments.size() != 3) {
                            throw new GyroException("@swap directive only takes 3 arguments!");
                        }

                        swaps.add(directive);
                        continue;
                }

            } else if (item instanceof ResourceNode) {
                ResourceNode r = (ResourceNode) item;

                if (r.getType().equals("transition")) {
                    transitions.add(new Transition(parent, r));
                    continue;
                }
            }

            evaluator.visit(item, scope);
        }

        name = (String) evaluator.visit(node.getName(), parent);
        confirmDiff = Boolean.TRUE.equals(scope.get("confirm-diff"));
        transitionPrompt = (String) scope.get("transition-prompt");
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

        for (DirectiveNode create : creates) {
            List<Object> arguments = DirectiveProcessor.resolveArguments(executeScope, create);

            evaluator.visit(
                new ResourceNode(
                    (String) arguments.get(0),
                    new ValueNode(arguments.get(1)),
                    create.getBody()),
                executeScope);
        }

        for (DirectiveNode delete : deletes) {
            List<Object> arguments = delete.getArguments()
                .stream()
                .map(a -> evaluator.visit(a, executeScope))
                .collect(Collectors.toList());

            String fullName = arguments.get(0) + "::" + getResourceName(arguments.get(1));

            pendingRootScope.getFileScopes().forEach(s -> s.remove(fullName));
        }

        for (DirectiveNode swap : swaps) {
            List<Object> arguments = swap.getArguments()
                .stream()
                .map(a -> evaluator.visit(a, executeScope))
                .collect(Collectors.toList());

            String type = (String) arguments.get(0);
            String x = getResourceName(arguments.get(1));
            String y = getResourceName(arguments.get(2));

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
