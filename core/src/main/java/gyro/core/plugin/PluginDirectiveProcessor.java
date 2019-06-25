package gyro.core.plugin;

import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.repo.RepositorySettings;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;
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

public class PluginDirectiveProcessor extends DirectiveProcessor {

    private static final ConcurrentMap<String, Set<Class<?>>> CLASSES_BY_ARTIFACT_COORDS = new ConcurrentHashMap<>();
    private static final PluginClassLoader PLUGIN_CLASS_LOADER = new PluginClassLoader();

    @Override
    public String getName() {
        return "plugin";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) throws Exception {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@plugin directive can only be used within the init.gyro file!");
        }

        List<Object> arguments = resolveArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@plugin directive only takes 1 argument!");
        }

        Thread.currentThread().setContextClassLoader(PLUGIN_CLASS_LOADER);

        PluginSettings settings = scope.getSettings(PluginSettings.class);
        String artifactCoords = (String) arguments.get(0);

        settings.addClasses(CLASSES_BY_ARTIFACT_COORDS.computeIfAbsent(artifactCoords, ac -> {
            try {
                GyroCore.ui().write("@|magenta ↓ Loading %s plugin|@\n", ac);

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

                Artifact artifact = new DefaultArtifact(ac);
                Dependency dependency = new Dependency(artifact, JavaScopes.RUNTIME);
                DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
                List<RemoteRepository> repositories = scope.getSettings(RepositorySettings.class).getRepositories();
                CollectRequest collectRequest = new CollectRequest(dependency, repositories);
                DependencyRequest request = new DependencyRequest(collectRequest, filter);
                DependencyResult result = system.resolveDependencies(session, request);

                PLUGIN_CLASS_LOADER.add(result);

                Set<Class<?>> classes = new LinkedHashSet<>();

                try (JarFile jar = new JarFile(result.getRoot().getArtifact().getFile())) {
                    for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                        JarEntry entry = e.nextElement();

                        if (entry.isDirectory()) {
                            continue;
                        }

                        String name = entry.getName();

                        if (!name.endsWith(".class")) {
                            continue;
                        }

                        name = name.substring(0, name.length() - 6);
                        name = name.replace('/', '.');
                        Class<?> c = Class.forName(name, false, PLUGIN_CLASS_LOADER);

                        int modifiers = c.getModifiers();

                        if (!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)) {
                            classes.add(c);
                        }
                    }
                }

                return classes;

            } catch (Exception error) {
                throw new GyroException(String.format(
                    "Can't load [%s] plugin! %s: %s",
                    ac,
                    error.getClass().getName(),
                    error.getMessage()));
            }
        }));
    }

}
