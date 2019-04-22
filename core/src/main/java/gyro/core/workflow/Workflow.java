package gyro.core.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import gyro.core.GyroUI;
import gyro.core.LocalFileBackend;
import gyro.core.diff.Diff;
import gyro.core.resource.Resource;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.ResourceNode;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;

public class Workflow {

    private final RootScope pendingRootScope;
    private final String name;
    private final String forType;
    private final List<Stage> stages = new ArrayList<>();

    public Workflow(Scope parent, ResourceNode node) throws Exception {
        Scope scope = new Scope(parent);

        for (Iterator<Node> i = node.getBody().iterator(); i.hasNext();) {
            Node item = i.next();

            if (item instanceof ResourceNode) {
                ResourceNode rn = (ResourceNode) item;

                if (rn.getType().equals("stage")) {
                    Stage stage = new Stage(scope, rn);

                    stages.add(stage);
                    i.remove();
                    continue;
                }
            }

            item.evaluate(scope);
        }

        pendingRootScope = parent.getRootScope();
        name = (String) node.getNameNode().evaluate(parent);
        forType = (String) scope.get("for-type");
    }

    public String getName() {
        return name;
    }

    public String getForType() {
        return forType;
    }

    private RootScope copyCurrentRootScope() throws Exception {
        RootScope s = new RootScope(pendingRootScope.getFile(), new HashSet<>(pendingRootScope.getActiveScopePaths()));

        new LocalFileBackend().load(s);
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

            new LocalFileBackend().load(currentRootScope);
            new LocalFileBackend().load(pendingRootScope);

            Diff diff = new Diff(currentRootScope.findAllActiveResources(), pendingRootScope.findAllActiveResources());

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
