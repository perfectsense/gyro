package gyro.core.directive;

import java.util.HashMap;
import java.util.Map;

import gyro.core.resource.Settings;

public class DirectiveSettings extends Settings {

    private Map<String, DirectiveProcessor> processors;

    public Map<String, DirectiveProcessor> getProcessors() {
        if (processors == null) {
            processors = new HashMap<>();
        }

        return processors;
    }

    public void setProcessors(Map<String, DirectiveProcessor> processors) {
        this.processors = processors;
    }

}
