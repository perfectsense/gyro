package gyro.core.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.lang.ast.block.DirectiveNode;

public class ExtendsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public String getName() {
        return "extends";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 1);

        processSource(scope, arguments.get(0));
        scope.setExtended(true);
    }

    @SuppressWarnings("unchecked")
    private void processSource(DiffableScope scope, Object source) {
        if (source instanceof Map) {
            ((Map<String, Object>) source).forEach(scope::putIfAbsent);

        } else if (source instanceof Resource) {
            Resource resource = (Resource) source;
            Set<String> configuredFields = resource.configuredFields;

            if (configuredFields == null) {
                configuredFields = ImmutableSet.of();
            }

            for (DiffableField field : DiffableType.getInstance(resource.getClass()).getFields()) {
                String name = field.getName();

                if (field.shouldBeDiffed() || configuredFields.contains(name)) {
                    scope.putIfAbsent(name, field.getValue(resource));
                }
            }

        } else if (source instanceof String) {
            String name = (String) source;
            Resource resource = scope.getRootScope().findResource(name);

            if (resource == null) {
                throw new GyroException(String.format(
                    "Can't extend from @|bold %s|@ resource because it doesn't exist!",
                    name));
            }

            processSource(scope, resource);
        }
    }

}
