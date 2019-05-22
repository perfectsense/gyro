package gyro.core.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;

public class Workflow {

    private final RootScope pendingRootScope;
    private final String name;
    private final String forType;
    private final List<Stage> stages = new ArrayList<>();

    public Workflow(Scope parent, ResourceNode node) {
        Scope scope = new Scope(parent);
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        for (Node item : node.getBody()) {
            if (item instanceof ResourceNode) {
                ResourceNode rn = (ResourceNode) item;

                if (rn.getType().equals("stage")) {
                    Stage stage = new Stage(scope, rn);

                    stages.add(stage);
                    continue;
                }
            }

            evaluator.visit(item, scope);
        }

        pendingRootScope = parent.getRootScope();
        name = (String) evaluator.visit(node.getName(), parent);
        forType = (String) scope.get("for-type");
    }

    public String getName() {
        return name;
    }

    public String getForType() {
        return forType;
    }

    private RootScope copyCurrentRootScope() throws Exception {
        RootScope s = new RootScope(
            pendingRootScope.getFile(),
            pendingRootScope.getBackend(),
            null,
            pendingRootScope.getLoadFiles());

        s.load();

        return s;
    }

    public void execute(
            GyroUI ui,
            State state,
            Resource pendingResource)
            throws Exception {

        int stagesSize = stages.size();

        if (stagesSize == 0) {
            throw new IllegalArgumentException("No stages!");
        }

        int stageIndex = 0;
        RootScope currentRootScope = copyCurrentRootScope();

        do {
            Stage stage = stages.get(stageIndex);
            String stageName = stage.getName();

            ui.write("\n@|magenta %d Executing %s stage|@\n", stageIndex + 1, stageName);

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            ui.indent();

            try {
                RootScope pendingRootScope = copyCurrentRootScope();
                stageName = stage.execute(ui, state, pendingResource, currentRootScope, pendingRootScope);
                currentRootScope = pendingRootScope;

            } finally {
                ui.unindent();
            }

            if (stageName == null) {
                ++stageIndex;

            } else {
                stageIndex = -1;

                for (int i = 0; i < stagesSize; ++i) {
                    Stage s = stages.get(i);

                    if (s.getName().equals(stageName)) {
                        stageIndex = i;
                        break;
                    }
                }

                if (stageIndex < 0) {
                    throw new IllegalArgumentException(String.format(
                            "No stage named [%s]!",
                            stageName));
                }
            }

        } while (stageIndex < stagesSize);

        ui.write("\n@|magenta ~ Finalizing %s workflow |@\n", name);
        ui.indent();

        try {
            currentRootScope = pendingRootScope.getCurrent();

            currentRootScope.clear();
            pendingRootScope.clear();

            currentRootScope.load();
            pendingRootScope.load();

            Set<String> diffFiles = state.getDiffFiles();

            Diff diff = new Diff(
                currentRootScope.findResourcesIn(diffFiles),
                pendingRootScope.findResourcesIn(diffFiles));

            diff.diff();

            if (diff.write(ui)) {
                if (ui.readBoolean(Boolean.TRUE, "\nFinalize %s workflow?", name)) {
                    ui.write("\n");
                    diff.executeCreateOrUpdate(ui, state);
                    diff.executeReplace(ui, state);
                    diff.executeDelete(ui, state);

                } else {
                    throw new RuntimeException("Aborted!");
                }
            }

        } finally {
            ui.unindent();
        }
    }

}
