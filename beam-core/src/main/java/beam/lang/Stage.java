package beam.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import beam.core.BeamUI;
import beam.core.diff.Diff;
import beam.lang.ast.Node;
import beam.lang.ast.block.KeyBlockNode;
import beam.lang.ast.block.ResourceNode;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.scope.State;

public class Stage {

    private final String name;
    private final String prompt;
    private final List<Node> changes = new ArrayList<>();
    private final List<KeyBlockNode> swaps = new ArrayList<>();
    private final List<Transition> transitions;

    @SuppressWarnings("unchecked")
    public Stage(Scope parent, ResourceNode node) throws Exception {
        Scope scope = new Scope(parent);

        for (Iterator<Node> i = node.getBody().iterator(); i.hasNext();) {
            Node item = i.next();

            if (item instanceof KeyBlockNode) {
                KeyBlockNode kb = (KeyBlockNode) item;
                String kbKey = kb.getKey();

                if (kbKey.equals("create")) {
                    changes.addAll(kb.getBody());
                    i.remove();
                    continue;

                } else if (kbKey.equals("delete")) {
                    changes.add(kb);
                    i.remove();
                    continue;

                } else if (kbKey.equals("swap")) {
                    swaps.add(kb);
                    i.remove();
                    continue;
                }
            }

            item.evaluate(scope);
        }

        name = (String) node.getNameNode().evaluate(parent);
        prompt = (String) scope.get("prompt");

        transitions = Optional.ofNullable((List<Map<String, Object>>) scope.get("transition"))
                .map(l -> l.stream()
                        .map(Transition::new)
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    public String getName() {
        return name;
    }

    public String execute(
            BeamUI ui,
            State state,
            Resource pendingResource,
            RootScope currentRootScope,
            RootScope pendingRootScope)
            throws Exception {

        Scope executeScope = new Scope(pendingRootScope);

        executeScope.put("NAME", pendingResource.resourceIdentifier());
        executeScope.put("PENDING", pendingResource.scope().resolve());

        for (Node change : changes) {
            change.evaluate(executeScope);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> deletes = (List<Map<String, Object>>) executeScope.get("delete");

        if (deletes != null) {
            for (Map<String, Object> delete : deletes) {
                pendingRootScope.remove(delete.get("type") + "::" + delete.get("name"));
            }
        }

        for (KeyBlockNode swap : swaps) {
            swap.evaluate(executeScope);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> swaps = (List<Map<String, Object>>) executeScope.get("swap");

        if (swaps != null) {
            for (Map<String, Object> swap : swaps) {
                String type = (String) swap.get("type");
                String x = (String) swap.get("x");
                String y = (String) swap.get("y");

                ui.write("@|magenta ⤢ Swapping %s with %s|@\n", x, y);
                state.swap(currentRootScope, pendingRootScope, type, x, y);
            }
        }

        Diff diff = new Diff(currentRootScope.findAllResources(), pendingRootScope.findAllResources());

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", name)) {
                ui.write("\n");
                diff.executeCreateOrUpdate(ui, state);
                diff.executeReplace(ui, state);
                diff.executeDelete(ui, state);

            } else {
                throw new RuntimeException("Aborted!");
            }
        }

        if (transitions == null) {
            return null;
        }

        Map<String, String> options = transitions.stream()
                .collect(Collectors.toMap(Transition::getName, Transition::getStage));

        while (true) {
            for (Transition transition : transitions) {
                ui.write("\n%s) %s", transition.getName(), transition.getDescription());
            }

            String selected = ui.readText("\n%s ", prompt);
            String selectedOption = options.get(selected);

            if (selectedOption != null) {
                return selectedOption;

            } else {
                ui.write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

}
