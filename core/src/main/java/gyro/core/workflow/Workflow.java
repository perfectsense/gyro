package gyro.core.workflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.Abort;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Diff;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.util.ImmutableCollectors;

public class Workflow {

    public static final String EXECUTION_FILE = "workflow-execution.json";

    private final String type;
    private final String name;
    private final RootScope root;
    private final Map<String, Stage> stages;

    public Workflow(String type, String name, Scope scope) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.root = Preconditions.checkNotNull(scope).getRootScope();

        @SuppressWarnings("unchecked")
        List<Scope> stageScopes = (List<Scope>) scope.get("stage");

        if (stageScopes.isEmpty()) {
            throw new GyroException("Workflow requires 1 or more stages!");
        }

        this.stages = stageScopes.stream()
            .map(s -> new Stage(scope.getName(s), s))
            .collect(ImmutableCollectors.toMap(Stage::getName));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getExecution(RootScope root) {
        try (GyroInputStream input = root.openInput(Workflow.EXECUTION_FILE)) {
            return (Map<String, String>) ObjectUtils.fromJson(IoUtils.toString(input, StandardCharsets.UTF_8));

        } catch (GyroException error) {
            return null;

        } catch (IOException error) {
            throw new GyroException(error);
        }
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Stage getStage(String name) {
        Stage stage = stages.get(name);

        if (stage == null) {
            throw new GyroException(String.format(
                "Can't find @|bold %s|@ stage in @|bold %s|@ workflow!",
                name,
                this.name));
        }

        return stage;
    }

    private RootScope copyCurrentRootScope() {
        RootScope current = root.getCurrent();
        RootScope scope = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles());
        scope.evaluate();
        return scope;
    }

    public void execute(
        GyroUI ui,
        State state,
        Resource currentResource,
        Resource pendingResource) {

        Map<String, String> execution = getExecution(root.getCurrent());

        Stage stage = execution != null
            ? getStage(execution.get("nextStage"))
            : stages.values().iterator().next();

        while (true) {
            ui.write("\n@|magenta · Executing %s stage|@\n", stage.getName());

            if (ui.isVerbose()) {
                ui.write("\n");
            }

            String nextStageName;
            RootScope currentRoot = copyCurrentRootScope();

            ui.indent();

            try {
                nextStageName = stage.execute(ui, state, currentResource, pendingResource, currentRoot, copyCurrentRootScope());

            } finally {
                ui.unindent();
            }

            if (nextStageName == null) {
                currentRoot.delete(EXECUTION_FILE);
                break;
            }

            try (GyroOutputStream output = currentRoot.openOutput(EXECUTION_FILE)) {
                output.write(ObjectUtils.toJson(ImmutableMap.of(
                    "type", DiffableType.getInstance(currentResource).getName(),
                    "name", DiffableInternals.getName(currentResource),
                    "workflow", name,
                    "currentStage", stage.getName(),
                    "nextStage", nextStageName
                )).getBytes(StandardCharsets.UTF_8));
            }

            stage = getStage(nextStageName);
        }

        RootScope current = copyCurrentRootScope();
        RootScope pending = new RootScope(root.getFile(), root.getBackend(), current, root.getLoadFiles());

        pending.evaluate();
        pending.validate();

        Diff diff = new Diff(
            current.findResourcesIn(current.getLoadFiles()),
            pending.findResourcesIn(pending.getLoadFiles()));

        diff.diff();

        if (diff.write(ui)) {
            if (ui.readBoolean(Boolean.TRUE, "\nFinalize %s workflow?", name)) {
                ui.write("\n");
                diff.execute(ui, state);

            } else {
                throw new Abort();
            }
        }
    }

}
