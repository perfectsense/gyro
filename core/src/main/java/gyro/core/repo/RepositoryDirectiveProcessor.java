package gyro.core.repo;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositoryDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "repository";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateArguments(scope, node, 1, 1);
        String url = (String) arguments.get(0);

        scope.getSettings(RepositorySettings.class)
            .getRepositories()
            .add(new RemoteRepository.Builder(url, "default", url).build());
    }

}
