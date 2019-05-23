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
    @SuppressWarnings("unchecked")
    public void process(Scope scope, List<Object> arguments) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        if (diffableScope == null) {
            throw new GyroException("@extends can only be used inside a resource!");
        }

        if (arguments.size() != 1) {
            throw new GyroException("@extends directive only takes 1 argument!");
        }

        Object source = arguments.get(0);
        RootScope rootScope = scope.getRootScope();

        if (source instanceof String) {
            String name = (String) source;
            Resource resource = rootScope.findResource(name);

            if (resource == null) {
                throw new GyroException(String.format(
                    "No resource named [%s]!",
                    name));
            }

            for (DiffableField field : DiffableType.getInstance(resource.getClass()).getFields()) {
                diffableScope.putIfAbsent(field.getName(), field.getValue(resource));
            }

        } else if (source instanceof Map) {
            ((Map<String, Object>) source).forEach(diffableScope::putIfAbsent);
        }
    }

}
