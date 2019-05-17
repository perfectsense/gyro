package gyro.core.plugin;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
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
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

public class PluginDirectiveProcessor implements DirectiveProcessor {

    private static final Map<String, Plugin> PLUGINS = new HashMap<>();

    @Override
    public String getName() {
        return "plugin";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) throws Exception {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@plugin directive can only be used within the init.gyro file!");
        }

        synchronized (PluginDirectiveProcessor.class) {
            String artifactCoords = (String) arguments.get(0);
            Plugin plugin = PLUGINS.get(artifactCoords);

            if (plugin == null) {
                GyroCore.ui().write("@|magenta ↓ Loading %s plugin|@\n", artifactCoords);

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

                Artifact artifact = new DefaultArtifact(artifactCoords);
                Dependency dependency = new Dependency(artifact, JavaScopes.RUNTIME);
                DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
                List<RemoteRepository> repositories = scope.getSettings(RepositorySettings.class).getRepositories();
                DependencyRequest request = new DependencyRequest(new CollectRequest(dependency, repositories), filter);
                DependencyResult result = system.resolveDependencies(session, request);
                plugin = new Plugin(result);

                PLUGINS.put(artifactCoords, plugin);
            }

            plugin.execute((RootScope) scope);
        }
    }

}
