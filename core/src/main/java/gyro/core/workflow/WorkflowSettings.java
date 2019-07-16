package gyro.core.workflow;

import java.util.ArrayList;
import java.util.List;

import gyro.core.scope.Settings;

public class WorkflowSettings extends Settings {

    private List<Workflow> workflows;
    private List<Action> actions;

    public List<Workflow> getWorkflows() {
        if (workflows == null) {
            workflows = new ArrayList<>();
        }

        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public List<Action> getActions() {
        if (actions == null) {
            actions = new ArrayList<>();
        }

        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

}
