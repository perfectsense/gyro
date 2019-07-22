package gyro.core.diff;

import java.util.ArrayList;
import java.util.List;

import gyro.core.scope.Settings;

public class ChangeSettings extends Settings {

    private List<ChangeProcessor> processors;

    public List<ChangeProcessor> getProcessors() {
        if (processors == null) {
            processors = new ArrayList<>();
        }

        return processors;
    }

    public void setProcessors(List<ChangeProcessor> processors) {
        this.processors = processors;
    }

}
