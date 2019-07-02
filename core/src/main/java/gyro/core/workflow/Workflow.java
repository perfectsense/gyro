package gyro.core.workflow;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Diff;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.util.ImmutableCollectors;

public class Workflow {

    private final String type;
    private final String name;
    private final RootScope root;
    private final List<Stage> stages;

    public Workflow(String type, String name, Scope scope) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.root = Preconditions.checkNotNull(scope).getRootScope();

        @SuppressWarnings("unchecked")
        List<Scope> stageScopes = (List<Scope>) scope.get("stage");

        if (stageScopes.isEmpty()) {
            throw new GyroException("Workflow requires stages!");
        }

        this.stages = stageScopes.stream()
            .map(s -> new Stage(scope.getName(s), s))
            .collect(ImmutableCollectors.toList());
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<Stage> getStages() {
        return stages;
    }

    private RootScope copyCurrentRootScope() throws Exception {
        RootScope current = root.getCurrent();
        RootScope scope = new RootScope(
            current.getFile(),
            current.getBackend(),
            null,
            current.getLoadFiles());

        scope.load();

        return scope;
    }

    public void execute(
            GyroUI ui,
            State state,
            Resource currentResource,
            Resource pendingResource)
            throws Exception {

        int stagesSize = stages.size();

        if (stagesSize == 0) {
            throw new IllegalArgumentException("No stages!");
        }

        int stageIndex = 0;

        do {
            Stage stage = stages.get(stageIndex);
            String stageName = stage.getName();

            ui.write("\n@|magenta %d Executing %s stage|@\n", stageIndex + 1, stageName);

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            ui.indent();

            try {
                stageName = stage.execute(ui, state, currentResource, pendingResource, copyCurrentRootScope(), copyCurrentRootScope());

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

        RootScope current = copyCurrentRootScope();

        RootScope pending = new RootScope(
            root.getFile(),
            root.getBackend(),
            current,
            root.getLoadFiles());

        pending.load();

        Set<String> diffFiles = state.getDiffFiles();

        Diff diff = new Diff(
            current.findResourcesIn(diffFiles),
            pending.findResourcesIn(diffFiles));

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
    }

}
