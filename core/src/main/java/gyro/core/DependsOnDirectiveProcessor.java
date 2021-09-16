package gyro.core;

import java.util.Collection;
import java.util.stream.Collectors;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.Resource;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("depends-on")
public class DependsOnDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 1, 1);

        DependsOnSettings settings = scope.getSettings(DependsOnSettings.class);

        if (!(scope instanceof DiffableScope) && !(scope.getParent() instanceof DiffableScope)) {
            throw new GyroException("@depends-on can only be used inside a resource.");
        }

        Object value = getArgument(scope, node, Object.class, 0);
        if (value instanceof Collection) {
            settings.getDependencies().addAll(((Collection<?>) value).stream()
                .filter(Resource.class::isInstance)
                .map(Resource.class::cast)
                .collect(Collectors.toSet()));
        } else if (value instanceof Resource) {
            settings.getDependencies().add((Resource) value);
        }
    }

}
