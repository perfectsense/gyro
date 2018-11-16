package beam.fetcher;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import beam.core.extensions.CredentialsExtension;
import beam.core.extensions.ResourceExtension;
import beam.lang.BCL;
import beam.lang.BeamConfig;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class MavenFetcher extends PluginFetcher {

    private static String MAVEN_KEY = "^(?<group>[^:]+):(?<artifactId>[^:]+):(?<version>[^:]+)";
    private static Pattern MAVEN_KEY_PAT = Pattern.compile(MAVEN_KEY);

    @Override
    public boolean validate(BeamConfig fetcherContext) {
        if (fetcherContext.get("artifact") != null) {
            String key = (String) fetcherContext.get("artifact").getValue();
            if (key != null) {
                return MAVEN_KEY_PAT.matcher(key).find();
            }
        }

        return false;
    }

    @Override
    public void fetch(BeamConfig fetcherContext) {
        try {
            String key = (String) fetcherContext.get("artifact").getValue();
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

            RepositorySystem system = locator.getService(RepositorySystem.class);
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
            session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

            List<RemoteRepository> remoteRepositories = new ArrayList<>();
            if (fetcherContext.get("repositories") != null && fetcherContext.get("repositories").getValue() != null) {
                List<String> repos = (List<String>) fetcherContext.get("repositories").getValue();
                for (String repo : repos) {
                    remoteRepositories.add(new RemoteRepository.Builder(repo, "default", repo).build());
                }
            } else {
                remoteRepositories.add(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());
            }

            Artifact artifact = new DefaultArtifact(key);

            CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), remoteRepositories);
            DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
            DependencyRequest request = new DependencyRequest(collectRequest, filter);
            DependencyResult result = system.resolveDependencies(session, request);

            ClassLoader parent = getClass().getClassLoader();
            List<URL> urls = new ArrayList<>();
            File artifactFile = null;
            for (ArtifactResult artifactResult : result.getArtifactResults()) {
                if (artifactFile == null) {
                    artifactFile = artifactResult.getArtifact().getFile();
                }

                urls.add(new URL("file:///" + artifactResult.getArtifact().getFile()));
            }

            URL[] url = urls.toArray(new URL[0]);
            URLClassLoader loader = new URLClassLoader(url, parent);

            JarFile jarFile = new JarFile(artifactFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                String className = StringUtils.removeEnd(entry.getName(), ".class");
                className = className.replace('/', '.');
                Class c = loader.loadClass(className);
                if (BeamResource.class.isAssignableFrom(c)) {
                    if (!Modifier.isAbstract(c.getModifiers())) {
                        BeamResource resource = (BeamResource) c.newInstance();
                        BeamCredentials credentials = (BeamCredentials) resource.getResourceCredentialsClass().newInstance();

                        String resourceNamespace = credentials.getName();
                        String resourceName = c.getSimpleName();

                        if (c.isAnnotationPresent(ResourceName.class)) {
                            ResourceName name = (ResourceName) c.getAnnotation(ResourceName.class);
                            resourceName = name.value();
                        }

                        String fullName = String.format("%s::%s", resourceNamespace, resourceName);
                        BCL.addExtension(fullName, new ResourceExtension(fullName, c));
                    }
                } else if (BeamCredentials.class.isAssignableFrom(c)) {
                    BCL.addExtension(c.getSimpleName(), new CredentialsExtension(c));
                }
            }
        } catch (Exception e) {
            throw new BeamException("Maven fetch failed!", e);
        }
    }
}
