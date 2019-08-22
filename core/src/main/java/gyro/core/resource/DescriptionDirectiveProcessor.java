package gyro.core.resource;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("description")
public class DescriptionDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        scope.getSettings(DescriptionSettings.class).setDescription(validateArguments(node, 1, 1).get(0));
    }

}
