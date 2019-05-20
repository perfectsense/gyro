package gyro.core.repo;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositoryDirectiveProcessor implements DirectiveProcessor {

    @Override
    public String getName() {
        return "repository";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@repository directive can only be used within the init.gyro file!");
        }

        String url = (String) arguments.get(0);

        scope.getSettings(RepositorySettings.class)
            .getRepositories()
            .add(new RemoteRepository.Builder(url, null, url).build());
    }

}
