package gyro.core.directive;

import java.util.HashMap;
import java.util.Map;

import gyro.core.scope.Scope;
import gyro.core.scope.Settings;

public class DirectiveSettings extends Settings {

    private Map<String, DirectiveProcessor<? extends Scope>> processors;

    public Map<String, DirectiveProcessor<? extends Scope>> getProcessors() {
        if (processors == null) {
            processors = new HashMap<>();
        }

        return processors;
    }

    public void setProcessors(Map<String, DirectiveProcessor<? extends Scope>> processors) {
        this.processors = processors;
    }

}
