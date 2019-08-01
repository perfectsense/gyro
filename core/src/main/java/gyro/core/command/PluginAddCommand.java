package gyro.core.command;

import gyro.core.GyroCore;
import gyro.core.repo.RepositorySettings;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.model.MetadataLoader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(name = "add", description = "Add one or more plugins.")
public class PluginAddCommand extends PluginCommand {

    private List<RemoteRepository> repositories;

    @Override
    protected void executeSubCommand() throws Exception {
        if (getPlugins().isEmpty()) {
            Help.help(MetadataLoader.loadCommand(PluginAddCommand.class));
            return;
        }

        GyroCore.ui().write("\n");

        Set<String> installedPlugins = getPlugins()
            .stream()
            .filter(f -> !pluginNotExist(f))
            .collect(Collectors.toSet());

        installedPlugins.stream()
            .map(p -> String.format("@|bold %s|@ is already installed.%n", p))
            .forEach(GyroCore.ui()::write);

        Set<String> plugins = getPlugins()
            .stream()
            .filter(this::pluginNotExist)
            .collect(Collectors.toSet());

        if (plugins.isEmpty()) {
            return;
        }

        if (repositories == null) {
            repositories = new ArrayList<>();

            repositories.add(RepositorySettings.CENTRAL);
        }

        getRepositoryNodes()
            .stream()
            .map(this::toRepositoryUrl)
            .forEach(s -> repositories.add(new RemoteRepository.Builder(s, "default", s).build()));

        plugins = plugins
            .stream()
            .filter(this::validate)
            .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        load()
            .stream()
            .map(l -> l + "\n")
            .forEach(sb::append);

        plugins.forEach(p -> sb.append(String.format("%s '%s'%n", "@plugin:", p)));
        save(sb.toString());

        plugins.stream()
            .map(p -> String.format("@|bold %s|@ has been added.%n", p))
            .forEach(GyroCore.ui()::write);
    }

    private boolean validate(String plugin) {
        try {
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

            RepositorySystem system = locator.getService(RepositorySystem.class);
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            String localDir = Paths.get(System.getProperty("user.home"), ".m2", "repository").toString();
            LocalRepository local = new LocalRepository(localDir);
            LocalRepositoryManager manager = system.newLocalRepositoryManager(session, local);

            session.setLocalRepositoryManager(manager);

            Artifact artifact = new DefaultArtifact(plugin);
            Dependency dependency = new Dependency(artifact, JavaScopes.RUNTIME);
            DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
            CollectRequest collectRequest = new CollectRequest(dependency, repositories);
            DependencyRequest request = new DependencyRequest(collectRequest, filter);
            system.resolveDependencies(session, request);

            return true;
        } catch (DependencyResolutionException e) {
            GyroCore.ui().write("@|bold %s|@ was not installed for the following reason(s):\n", plugin);

            for (Exception ex : e.getResult().getCollectExceptions()) {
                GyroCore.ui().write("   @|red %s|@\n", ex.getMessage());
            }

            GyroCore.ui().write("\n");

            return false;
        }
    }
}
