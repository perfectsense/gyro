package beam.core;

import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
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
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BeamProvider {

    private String name;
    private String artifact;
    private List<String> repositories;
    private BeamCore core;

    private static Map<String, Map<String, Class>> PROVIDER_CLASS_CACHE = new HashMap<>();
    private static Map<String, List<Artifact>> ARTIFACTS = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public List<String> getRepositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }

        return repositories;
    }

    public BeamCore getCore() {
        return core;
    }

    public void setCore(BeamCore core) {
        this.core = core;
    }

    public void load() {
        try {
            List<Artifact> artifacts = ARTIFACTS.get(getArtifact());
            if (artifacts == null) {
                BeamCore.ui().write("@|bold,blue Loading:|@ provider %s...\n", getName());

                List<RemoteRepository> remoteRepositories = new ArrayList<>();
                remoteRepositories.add(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());

                for (String repo : getRepositories()) {
                    remoteRepositories.add(new RemoteRepository.Builder(repo, "default", repo).build());
                }

                artifacts = fetchArtifacts(getArtifact(), remoteRepositories);

                ARTIFACTS.put(getArtifact(), artifacts);

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

                    if (getArtifact().equals(key)) {
                        registerResources(getCore(), artifactJarUrls, dependencyArtifact);
                    }
                }
            } else {
                for (String resourceName : PROVIDER_CLASS_CACHE.get(getArtifact()).keySet()) {
                    getCore().addResourceType(resourceName, PROVIDER_CLASS_CACHE.get(getArtifact()).get(resourceName));
                }
            }
        } catch (Exception e) {
            throw new BeamException("Maven fetch failed!", e);
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
        for (ArtifactResult  artifactResult: result.getArtifactResults()) {
            artifacts.add(artifactResult.getArtifact());
        }

        return artifacts;
    }

    private void registerResources(BeamCore core, URL[] urls, Artifact artifact) throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        URLClassLoader loader = new URLClassLoader(urls, parent);

        JarFile jarFile = new JarFile(artifact.getFile());
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }

            String className = StringUtils.removeEnd(entry.getName(), ".class");
            className = className.replace('/', '.');

            Class<?> resourceClass;
            try {
                resourceClass = loader.loadClass(className);
            } catch (Exception | Error ex) {
                continue;
            }

            if (Modifier.isAbstract(resourceClass.getModifiers())) {
                continue;
            }

            BeamCredentials credentials = null;
            if (BeamResource.class.isAssignableFrom(resourceClass)) {
                BeamResource resource = (BeamResource) resourceClass.newInstance();
                credentials = (BeamCredentials) resource.resourceCredentialsClass().newInstance();
            } else if (BeamCredentials.class.isAssignableFrom(resourceClass)) {
                credentials = (BeamCredentials) resourceClass.newInstance();
            } else {
                continue;
            }

            String resourceNamespace = credentials.getCloudName();
            for (ResourceName name : resourceClass.getAnnotationsByType(ResourceName.class)) {
                registerResource(core, artifact, resourceClass, resourceNamespace, name.value(), name.parent());
            }
        }
    }

    private void registerResource(BeamCore core, Artifact artifact,
                                  Class resourceClass, String resourceNamespace, String resourceName, String parentName) {
        String key = String.format("%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        Map<String, Class> cache = PROVIDER_CLASS_CACHE.get(key);
        if (cache == null) {
            cache = new HashMap<>();
            PROVIDER_CLASS_CACHE.put(key, cache);
        }

        String fullName = String.format("%s::%s", resourceNamespace, resourceName);
        if (!ObjectUtils.isBlank(parentName)) {
            fullName = String.format("%s::%s::%s", resourceNamespace, parentName, resourceName);
        }

        if (!cache.containsKey(fullName)) {
            cache.put(fullName, resourceClass);
        }

        core.addResourceType(fullName, cache.get(fullName));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("provider ");
        sb.append(getName()).append("\n");
        sb.append("    artifact: ");
        sb.append("'").append(getArtifact()).append("'");
        sb.append("\n");

        if (!getRepositories().isEmpty()) {
            sb.append("    repositories: [\n");

            List<String> values = new ArrayList<>();
            for (String value : getRepositories()) {
                values.add("        '" + value + "'");
            }

            sb.append(StringUtils.join(values, ",\n"));
            sb.append("\n    ]\n");
        }

        sb.append("end\n");

        return sb.toString();
    }

}
