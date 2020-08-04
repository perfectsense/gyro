package gyro.core.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.psddev.dari.util.StringUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.preprocessor.Preprocessor;
import gyro.core.repo.RepositorySettings;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositoryCache;
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
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

public class PluginPreprocessor extends Preprocessor {

    @Override
    public List<Node> preprocess(List<Node> nodes, RootScope scope) {
        PluginSettings settings = scope.getSettings(PluginSettings.class);

        List<String> artifactCoords = new ArrayList<>();
        List<Node> repositoryNodes = new ArrayList<>();

        for (Node node : nodes) {
            if (node instanceof DirectiveNode) {
                if ("plugin".equals(((DirectiveNode) node).getName())) {
                    artifactCoords.add(getArtifactCoord((DirectiveNode) node));
                } else if ("repository".equals(((DirectiveNode) node).getName())) {
                    repositoryNodes.add(node);
                }
            }
        }

        String cacheKey = StringUtils.hex(StringUtils.md5(StringUtils.join(artifactCoords, " ")));
        Path cachePath = Paths.get(System.getProperty("user.home"), ".gyro", "cache", cacheKey);
        Path cachedDependencyInfoPath = cachePath.resolve("deps");
        Path cachedArtifactInfoPath = cachePath.resolve("info");
        settings.setCachePath(cachePath);

        if (artifactCoords.stream().allMatch(settings::pluginInitialized)) {
            return nodes;
        }

        try {
            // Use $HOME/.gyro/cache/<key> to load jars if it exists.
            if (Files.exists(cachedDependencyInfoPath)) {
                PluginClassLoader classLoader = settings.getPluginClassLoader();

                boolean refreshDependencies = false;

                List<URL> jars = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(cachedDependencyInfoPath.toFile()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        if (!new File(line).exists()) {
                            refreshDependencies = true;
                            break;
                        }

                        jars.add(new URL("file:///" + line));
                    }
                }

                if (!refreshDependencies) {
                    classLoader.add(jars);
                    return nodes;
                }
            }

            Files.deleteIfExists(cachedDependencyInfoPath);
            Files.deleteIfExists(cachedArtifactInfoPath);
            Files.createDirectories(cachePath);
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        NodeEvaluator evaluator = new NodeEvaluator();
        evaluator.evaluate(scope, repositoryNodes);

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
        session.setCache(new DefaultRepositoryCache());

        for (String ac : artifactCoords) {
            if (settings.pluginInitialized(ac)) {
                continue;
            }

            try {
                GyroCore.ui().write("@|magenta ↓ Loading plugin:|@ %s\n", ac);

                Artifact pluginArtifact = new DefaultArtifact(ac);
                Dependency dependency = new Dependency(pluginArtifact, JavaScopes.RUNTIME);
                DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
                List<RemoteRepository> repositories = scope.getSettings(RepositorySettings.class).getRepositories();
                CollectRequest collectRequest = new CollectRequest(dependency, repositories);
                DependencyRequest request = new DependencyRequest(collectRequest, filter);
                DependencyResult result = system.resolveDependencies(session, request);

                settings.putDependencyResult(ac, result);

                // Write file to map ac to a file for later loading.
                Path file = result.getRoot().getArtifact().getFile().toPath();

                BufferedWriter writer = new BufferedWriter(new FileWriter(cachedArtifactInfoPath.toFile(), true));
                writer.newLine();
                writer.write(ac + " " + file.toString());
                writer.close();

                for (ArtifactResult artifactResult : result.getArtifactResults()) {
                    settings.putArtifactIfNewer(artifactResult.getArtifact());
                }
            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't load the @|bold %s|@ plugin!", ac),
                    error);
            }
        }

        settings.addAllUrls();

        // -- Build local cache of dependency data.
        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(cachedDependencyInfoPath.toFile(), true));
            for (URL url : settings.getPluginClassLoader().getPluginUrls()) {
                writer.write(url.toURI().getPath());
                writer.newLine();
            }
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return nodes;
    }

    private String getArtifactCoord(DirectiveNode node) {
        NodeEvaluator evaluator = new NodeEvaluator();
        Scope scope = new Scope(null);

        return (String) evaluator.visit(node.getArguments().get(0), scope);
    }
}
