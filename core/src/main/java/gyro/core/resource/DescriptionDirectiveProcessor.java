package gyro.core.resource;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

public class DescriptionDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public String getName() {
        return "description";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        scope.getSettings(DescriptionSettings.class).setDescription(validateDirectiveArguments(node, 1, 1).get(0));
    }

}
