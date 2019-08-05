package gyro.core.backend;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

public class UsesFileBackendDirectiveProcessor extends DirectiveProcessor<DiffableScope> {
    @Override
    public String getName() {
        return "uses-file-backend";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 1, 1);
        scope.getSettings(FileBackendSettings.class).setUseFileBackend(getArgument(scope, node, String.class, 0));
    }
}
