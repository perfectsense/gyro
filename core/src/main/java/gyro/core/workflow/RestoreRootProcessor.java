package gyro.core.workflow;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeReference;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.Defer;
import gyro.core.scope.RootProcessor;
import gyro.core.scope.RootScope;

public class RestoreRootProcessor extends RootProcessor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {

    };

    private static final TypeReference<List<String>> LIST_TYPE_REFERENCE = new TypeReference<>() {

    };

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

        String resourceType = null;
        String resourceName = null;
        Resource resource = null;
        String workflowName = null;
        List<String> executedStageNames = null;

        for (Map.Entry<String, Object> entry : execution.entrySet()) {
            resourceType = entry.getKey();
            Map<String, Object> currentExecution = ObjectUtils.to(MAP_TYPE_REFERENCE, entry.getValue());

            if (currentExecution != null) {
                resourceName = ObjectUtils.to(String.class, currentExecution.get("name"));
                resource = root.findResource(resourceType + "::" + resourceName);
                workflowName = ObjectUtils.to(String.class, currentExecution.get("workflow"));
                executedStageNames = ObjectUtils.to(LIST_TYPE_REFERENCE, currentExecution.get("executedStages"));

                if (resource != null) {
                    break;
                }
            }
        }

        if (resource == null) {
            throw new GyroException(String.format("Can't find @|bold %s|@ @|bold %s|@!", resourceType, resourceName));
        }

        String finalResourceType = resourceType;
        String finalWorkflowName = workflowName;

        Workflow workflow = root.getSettings(WorkflowSettings.class)
            .getWorkflows()
            .stream()
            .filter(w -> finalResourceType.equals(w.getType()) && finalWorkflowName.equals(w.getName()))
            .findFirst()
            .orElseThrow(() -> new Defer(null, String.format(
                "Can't restore @|bold %s|@ workflow because it doesn't exist!",
                finalWorkflowName)));

        GyroUI ui = GyroCore.ui();

        ui.write(
            "@|magenta ~ Restoring workflow:|@ @|bold %s|@ stages in @|bold %s|@ for replacing @|bold %s|@ @|bold %s|@\n",
            String.join(", ", executedStageNames),
            workflowName,
            resourceType,
            resourceName);
    }

}
