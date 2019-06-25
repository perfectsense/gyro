package gyro.core.repo;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositoryDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "repository";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@repository directive can only be used within the init.gyro file!");
        }

        List<Object> arguments = resolveArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@repository directive only takes 1 argument!");
        }

        String url = (String) arguments.get(0);

        scope.getSettings(RepositorySettings.class)
            .getRepositories()
            .add(new RemoteRepository.Builder(url, "default", url).build());
    }

}
