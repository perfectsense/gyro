package beam.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import beam.core.BeamUI;
import beam.core.LocalFileBackend;
import beam.core.diff.Diff;
import beam.lang.ast.Node;
import beam.lang.ast.block.KeyBlockNode;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.scope.State;

public class Stage {

    private final Scope scope;
    private final String name;
    private final String prompt;
    private final List<Node> creates = new ArrayList<>();
    private final List<KeyBlockNode> swaps = new ArrayList<>();
    private final List<Transition> transitions;

    @SuppressWarnings("unchecked")
    public Stage(Scope parent, List<Node> body) throws Exception {
        scope = new Scope(parent);

        for (Iterator<Node> i = body.iterator(); i.hasNext();) {
            Node node = i.next();

            if (node instanceof KeyBlockNode) {
                KeyBlockNode kb = (KeyBlockNode) node;
                String kbKey = kb.getKey();

                if (kbKey.equals("create")) {
                    creates.addAll(kb.getBody());
                    i.remove();
                    continue;

                } else if (kbKey.equals("swap")) {
                    swaps.add(kb);
                    i.remove();
                    continue;
                }
            }

            node.evaluate(scope);
        }

        name = (String) scope.get("name");
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

    public String execute(BeamUI ui, State state, Map<String, Object> pendingValues) throws Exception {
        Scope executeScope = new Scope(scope);

        executeScope.putAll(pendingValues);

        for (Node create : creates) {
            create.evaluate(executeScope);
        }

        for (KeyBlockNode swap : swaps) {
            swap.evaluate(executeScope);
        }

        RootScope pendingRootScope = scope.getRootScope();
        RootScope currentRootScope = pendingRootScope.getCurrent();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> swaps = (List<Map<String, Object>>) executeScope.get("swap");

        if (swaps != null) {
            for (Map<String, Object> swap : swaps) {
                swapResources(
                        currentRootScope,
                        (String) swap.get("type"),
                        (String) swap.get("x"),
                        (String) swap.get("y"));

                swapResources(
                        pendingRootScope,
                        (String) swap.get("type"),
                        (String) swap.get("x"),
                        (String) swap.get("y"));
            }
        }

        if (transitions == null) {
            pendingRootScope.clear();
            new LocalFileBackend().load(pendingRootScope);
        }

        Diff diff = new Diff(currentRootScope.findAllResources(), pendingRootScope.findAllResources());

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nContinue with %s stage?", name)) {
                ui.write("\n");
                diff.executeCreateOrUpdate(ui, state);
                diff.executeReplace(ui, state);
                diff.executeDelete(ui, state);
                currentRootScope.clear();
                new LocalFileBackend().load(currentRootScope);

            } else {
                return null;
            }

        } else {
            ui.write("\n@|yellow No changes in this stage.|@\n\n");
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

    private void swapResources(RootScope rootScope, String type, String xName, String yName) {
        String xFullName = type + "::" + xName;
        String yFullName = type + "::" + yName;
        FileScope xScope = find(xFullName, rootScope);
        FileScope yScope = find(yFullName, rootScope);
        Resource x = (Resource) xScope.get(xFullName);
        Resource y = (Resource) yScope.get(yFullName);

        xScope.put(xFullName, y);
        yScope.put(yFullName, x);
        x.resourceIdentifier(yName);
        y.resourceIdentifier(xName);
    }

    private FileScope find(String name, FileScope scope) {
        Object value = scope.get(name);

        if (value instanceof Resource) {
            return scope;
        }

        for (FileScope i : scope.getImports()) {
            FileScope r = find(name, i);

            if (r != null) {
                return r;
            }
        }

        return null;
    }

}
