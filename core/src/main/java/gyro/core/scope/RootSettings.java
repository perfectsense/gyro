package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;

public class RootSettings extends Settings {

    private List<RootProcessor> processors;

    public List<RootProcessor> getProcessors() {
        if (processors == null) {
            processors = new ArrayList<>();
        }
        return processors;
    }

    public void setProcessors(List<RootProcessor> processors) {
        this.processors = processors;
    }

}
