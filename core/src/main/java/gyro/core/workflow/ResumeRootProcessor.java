package gyro.core.workflow;

import java.io.IOException;
import java.util.Map;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.scope.RootProcessor;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.RootScope;

public class ResumeRootProcessor extends RootProcessor {

    @Override
    public void process(RootScope root) throws IOException {
        RootScope current = root.getCurrent();

        if (current == null) {
            return;
        }

        Map<String, String> execution = Workflow.getExecution(current);

        if (execution == null) {
            return;
        }

        String resourceType = execution.get("type");
        String resourceName = execution.get("name");
        Resource resource = root.findResource(resourceType + "::" + resourceName);

        if (resource == null) {
            throw new GyroException(String.format(
                "Can't find @|bold %s|@ @|bold %s|@!",
                resourceType,
                resourceName));
        }

        String workflowName = execution.get("workflow");

        Workflow workflow = root.getSettings(WorkflowSettings.class)
            .getWorkflows()
            .stream()
            .filter(w -> resourceType.equals(w.getType()) && workflowName.equals(w.getName()))
            .findFirst()
            .orElseThrow(() -> new Defer(null, String.format(
                "Can't resume @|bold %s|@ workflow because it doesn't exist!",
                workflowName)));

        String stageName = execution.get("currentStage");
        Stage stage = workflow.getStage(stageName);
        GyroUI ui = GyroCore.ui();

        ui.write(
            "\nResuming @|bold %s|@ workflow from @|bold %s|@ stage for @|bold %s|@ @|bold %s|@ resource\n",
            workflowName,
            stageName,
            resourceType,
            resourceName);

        stage.apply(
            ui,
            null,
            current.findResource(resourceType + "::" + resourceName),
            resource,
            root);
    }

}
