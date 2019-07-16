package gyro.core.repo;

import java.util.ArrayList;
import java.util.List;

import gyro.core.scope.Settings;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositorySettings extends Settings {

    /**
     * The default Maven central repository.
     *
     * @see <a href="https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Super_POM">Super POM</a>
     */
    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

    private List<RemoteRepository> repositories;

    public List<RemoteRepository> getRepositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();

            repositories.add(CENTRAL);
        }

        return repositories;
    }

    public void setRepositories(List<RemoteRepository> repositories) {
        this.repositories = repositories;
    }

}
