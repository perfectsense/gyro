package gyro.core.directive;

import java.util.HashMap;
import java.util.Map;

import gyro.core.Reflections;
import gyro.core.scope.Scope;
import gyro.core.scope.Settings;

public class DirectiveSettings extends Settings {

    private final Map<String, DirectiveProcessor<? extends Scope>> processors = new HashMap<>();

    public DirectiveProcessor<? extends Scope> getProcessor(String type) {
        return processors.get(type);
    }

    public void addProcessor(Class<? extends DirectiveProcessor<? extends Scope>> processorClass) {
        DirectiveProcessor<? extends Scope> processor = Reflections.newInstance(processorClass);
        processors.put(processor.getName(), processor);
    }

}
