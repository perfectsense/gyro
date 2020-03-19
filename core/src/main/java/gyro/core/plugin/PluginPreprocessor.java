package gyro.core.plugin;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

        if (artifactCoords.stream().allMatch(settings::pluginInitialized)) {
            return nodes;
        }

        NodeEvaluator evaluator = new NodeEvaluator();
        evaluator.evaluate(scope, repositoryNodes);

        for (String ac : artifactCoords) {
            if (settings.pluginInitialized(ac)) {
                continue;
            }

            try {
                GyroCore.ui().write("@|magenta ↓ Loading plugin:|@ %s\n", ac);

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

                settings.putDependencyResult(ac, result);

                for (ArtifactResult ar : result.getArtifactResults()) {
                    settings.putArtifactIfNewer(ar.getArtifact());
                }
            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't load the @|bold %s|@ plugin!", ac),
                    error);
            }
        }

        settings.addAllUrls();

        return nodes;
    }

    private String getArtifactCoord(DirectiveNode node) {
        NodeEvaluator evaluator = new NodeEvaluator();
        Scope scope = new Scope(null);

        return (String) evaluator.visit(node.getArguments().get(0), scope);
    }
}
