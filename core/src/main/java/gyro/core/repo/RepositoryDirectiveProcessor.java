package gyro.core.repo;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;
import org.eclipse.aether.repository.RemoteRepository;

@Type("repository")
public class RepositoryDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        String url = getArgument(scope, node, String.class, 0);

        scope.getSettings(RepositorySettings.class)
            .getRepositories()
            .add(new RemoteRepository.Builder(url, "default", url).build());
    }

}
