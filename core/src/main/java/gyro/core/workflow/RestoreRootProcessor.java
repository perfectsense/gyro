package gyro.core.workflow;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.RootProcessor;
import gyro.core.scope.RootScope;

public class RestoreRootProcessor extends RootProcessor {

    @Override
    public void process(RootScope root) throws IOException {
        RootScope current = root.getCurrent();

        if (current == null || root.isInWorkflow()) {
            return;
        }

        Map<String, Object> execution = Workflow.getExecution(current);

        if (execution == null) {
            return;
        }

        String resourceType = (String) execution.get("type");
        String resourceName = (String) execution.get("name");
        Resource resource = root.findResource(resourceType + "::" + resourceName);

        if (resource == null) {
            throw new GyroException(String.format(
                "Can't find @|bold %s|@ @|bold %s|@!",
                resourceType,
                resourceName));
        }

        String workflowName = (String) execution.get("workflow");

        root.getSettings(WorkflowSettings.class)
            .getWorkflows()
            .stream()
            .filter(w -> resourceType.equals(w.getType()) && workflowName.equals(w.getName()))
            .findFirst()
            .orElseThrow(() -> new Defer(null, String.format(
                "Can't restore @|bold %s|@ workflow because it doesn't exist!",
                workflowName)));

        GyroUI ui = GyroCore.ui();
        @SuppressWarnings("unchecked")
        List<String> executedStageNames = (List<String>) execution.get("executedStages");

        ui.write(
            "@|magenta ~ Restoring workflow:|@ @|bold %s|@ stages in @|bold %s|@ for replacing @|bold %s|@ @|bold %s|@\n",
            String.join(", ", executedStageNames),
            workflowName,
            resourceType,
            resourceName);
    }

}
