package beam.lang.plugins;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.lang.ast.scope.Scope;
import com.psddev.dari.util.StringUtils;
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
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {

    private String artifact;
    private List<String> repositories;
    private Scope scope;
    private static PluginClassLoader classLoader;

    private static Map<String, List<Artifact>> ARTIFACTS = new HashMap<>();
    private static Map<String, List<Class<?>>> PLUGIN_CLASS_CACHE = new HashMap<>();

    public static PluginClassLoader classLoader() {
        return classLoader;
    }

    @SuppressWarnings("unchecked")
    public PluginLoader(Scope scope) {
        this.scope = scope;
        this.artifact = (String) scope.get("artifact");
        this.repositories = (List<String>) scope.get("repositories");
    }

    public void artifact(String artifact) {
        this.artifact = artifact;
    }

    public String artifact() {
        return artifact;
    }

    public void repositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public List<String> repositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }

        return repositories;
    }

    public List<Artifact> artifacts() {
        return ARTIFACTS.get(artifact());
    }

    public List<Class<?>> classes() {
        return PLUGIN_CLASS_CACHE.getOrDefault(artifact, new ArrayList<>());
    }

    public Scope scope() {
        return scope;
    }

    public void load() {
        try {
            List<Artifact> artifacts = artifacts();
            if (artifacts == null) {
                BeamCore.ui().write("@|bold,blue Loading plugin:|@ %s...\n", artifact());

                List<RemoteRepository> remoteRepositories = new ArrayList<>();
                remoteRepositories.add(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());

                for (String repo : repositories()) {
                    remoteRepositories.add(new RemoteRepository.Builder(repo, "default", repo).build());
                }

                artifacts = fetchArtifacts(artifact(), remoteRepositories);

                ARTIFACTS.put(artifact(), artifacts);

                List<URL> artifactJars = new ArrayList<>();
                for (Artifact artifact : artifacts) {
                    artifactJars.add(new URL("file:///" + artifact.getFile()));
                }
                URL[] artifactJarUrls = artifactJars.toArray(new URL[0]);

                for (Artifact dependencyArtifact : artifacts) {
                    String key = String.format("%s:%s:%s",
                        dependencyArtifact.getGroupId(),
                        dependencyArtifact.getArtifactId(),
                        dependencyArtifact.getVersion());

                    if (artifact().equals(key)) {
                        loadClasses(artifactJarUrls, dependencyArtifact);
                    }
                }
            }
        } catch (Exception e) {
            throw new BeamException(e.getMessage());
        }

        init();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PluginLoader)) {
            return false;
        }

        PluginLoader loader = (PluginLoader) o;
        return artifact.equals(loader.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("plugin\n");
        sb.append("    artifact: ");
        sb.append("'").append(artifact()).append("'");
        sb.append("\n");

        if (!repositories().isEmpty()) {
            sb.append("    repositories: [\n");

            List<String> values = new ArrayList<>();
            for (String value : repositories()) {
                values.add("        '" + value + "'");
            }

            sb.append(StringUtils.join(values, ",\n"));
            sb.append("\n    ]\n");
        }

        sb.append("end\n");

        return sb.toString();
    }

    private void loadClasses(URL[] urls, Artifact artifact) throws Exception {
        ClassLoader parent = scope().getClass().getClassLoader();
        if (classLoader == null) {
            classLoader = new PluginClassLoader(urls, parent);
        } else {
            classLoader.addAllUrls(urls);
        }

        JarFile jarFile = new JarFile(artifact.getFile());
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }

            String className = StringUtils.removeEnd(entry.getName(), ".class");
            className = className.replace('/', '.');

            Class<?> loadedClass;
            try {
                loadedClass = classLoader.loadClass(className);
            } catch (Exception | Error ex) {
                continue;
            }

            List<Class<?>> cache = PLUGIN_CLASS_CACHE.computeIfAbsent(artifact(), f -> new ArrayList<>());
            cache.add(loadedClass);
        }
    }

    private List<Artifact> fetchArtifacts(String artifactKey, List<RemoteRepository> repositories) throws Exception {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        RepositorySystem system = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        Artifact artifact = new DefaultArtifact(artifactKey);

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), repositories);
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest request = new DependencyRequest(collectRequest, filter);
        DependencyResult result = system.resolveDependencies(session, request);

        List<Artifact> artifacts = new ArrayList<>();
        for (ArtifactResult artifactResult: result.getArtifactResults()) {
            artifacts.add(artifactResult.getArtifact());
        }

        return artifacts;
    }

    private void init() {
        List<Class<?>> cache = PLUGIN_CLASS_CACHE.computeIfAbsent(artifact(), f -> new ArrayList<>());

        for (Class loadedClass : cache) {
            if (Plugin.class.isAssignableFrom(loadedClass)) {
                loadPlugin(loadedClass);
            }
        }
    }

    private void loadPlugin(Class pluginClass) {
        List<Class<?>> cache = PLUGIN_CLASS_CACHE.computeIfAbsent(artifact(), f -> new ArrayList<>());

        try {
            Plugin plugin = (Plugin) pluginClass.newInstance();
            plugin.setScope(scope());
            plugin.artifact(artifact());

            for (Class loadedClass : cache) {
                plugin.classLoaded(loadedClass);
            }

            plugin.init();
        } catch (InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

}
