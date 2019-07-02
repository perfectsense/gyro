package gyro.core.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.lang.ast.block.DirectiveNode;

public class ExtendsDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "extends";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        if (diffableScope == null) {
            throw new GyroException("@extends can only be used inside a resource!");
        }

        List<Object> arguments = evaluateArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@extends directive only takes 1 argument!");
        }

        processSource(diffableScope, arguments.get(0));
        diffableScope.setExtended(true);
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
                    "No resource named [%s]!",
                    name));
            }

            processSource(scope, resource);
        }
    }

}
