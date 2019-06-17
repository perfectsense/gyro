package gyro.core.resource;

import java.util.List;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;

public class ExtendsDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "extends";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        if (diffableScope == null) {
            throw new GyroException("@extends can only be used inside a resource!");
        }

        if (arguments.size() != 1) {
            throw new GyroException("@extends directive only takes 1 argument!");
        }

        processSource(diffableScope, arguments.get(0));
    }

    @SuppressWarnings("unchecked")
    private void processSource(DiffableScope scope, Object source) {
        if (source instanceof List) {
            List<Object> list = (List<Object>) source;
            int size = list.size();

            if (size != 1) {
                throw new GyroException(String.format(
                    "Can't extend from [%s] items!",
                    size));
            }

            processSource(scope, list.get(0));

        } else if (source instanceof Map) {
            ((Map<String, Object>) source).forEach(scope::putIfAbsent);

        } else if (source instanceof Resource) {
            Resource resource = (Resource) source;

            for (DiffableField field : DiffableType.getInstance(resource.getClass()).getFields()) {
                scope.putIfAbsent(field.getName(), field.getValue(resource));
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
