package gyro.core.workflow;

import java.util.ArrayList;
import java.util.List;

import gyro.core.resource.Settings;

public class WorkflowSettings extends Settings {

    private List<Workflow> workflows;
    private List<Create> creates;
    private List<Delete> deletes;
    private List<Swap> swaps;

    public List<Workflow> getWorkflows() {
        if (workflows == null) {
            workflows = new ArrayList<>();
        }

        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public List<Create> getCreates() {
        if (creates == null) {
            creates = new ArrayList<>();
        }

        return creates;
    }

    public void setCreates(List<Create> creates) {
        this.creates = creates;
    }

    public List<Delete> getDeletes() {
        if (deletes == null) {
            deletes = new ArrayList<>();
        }

        return deletes;
    }

    public void setDeletes(List<Delete> deletes) {
        this.deletes = deletes;
    }

    public List<Swap> getSwaps() {
        if (swaps == null) {
            swaps = new ArrayList<>();
        }

        return swaps;
    }

    public void setSwaps(List<Swap> swaps) {
        this.swaps = swaps;
    }

}
