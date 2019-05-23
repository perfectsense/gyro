package gyro.core.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroUI;
import gyro.core.resource.Diff;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.DirectiveNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;

public class Stage {

    private final String name;
    private final boolean confirmDiff;
    private final String transitionPrompt;
    private final List<Node> changes = new ArrayList<>();
    private final List<DirectiveNode> swaps = new ArrayList<>();
    private final List<Transition> transitions = new ArrayList<>();

    public Stage(Scope parent, ResourceNode node) {
        Scope scope = new Scope(parent);
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        for (Node item : node.getBody()) {
            if (item instanceof DirectiveNode) {
                DirectiveNode d = (DirectiveNode) item;

                if (d.getName().equals("swap")) {
                    swaps.add(d);
                    continue;
                }

            } else if (item instanceof KeyBlockNode) {
                KeyBlockNode kb = (KeyBlockNode) item;
                String kbKey = kb.getKey();

                if (kbKey.equals("create")) {
                    changes.addAll(kb.getBody());
                    continue;

                } else if (kbKey.equals("delete")) {
                    changes.add(kb);
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
        executeScope.put("PENDING", pendingResource.scope().resolve());

        for (Node change : changes) {
            evaluator.visit(change, executeScope);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> deletes = (List<Map<String, Object>>) executeScope.get("delete");

        if (deletes != null) {
            for (Map<String, Object> delete : deletes) {
                pendingRootScope.remove(delete.get("type") + "::" + delete.get("name"));
            }
        }

        for (DirectiveNode swap : swaps) {
            List<Object> arguments = swap.getArguments()
                .stream()
                .map(a -> evaluator.visit(a, executeScope))
                .collect(Collectors.toList());

            String type = (String) arguments.get(0);
            String x = (String) arguments.get(1);
            String y = (String) arguments.get(2);

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
                throw new RuntimeException("Aborted!");
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
