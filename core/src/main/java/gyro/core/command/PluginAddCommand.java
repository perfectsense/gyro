/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.command;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.GyroCore;
import gyro.core.repo.RepositorySettings;
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
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "add",
    header = "Add one or more plugins to this project.",
    synopsisHeading = "%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    usageHelpWidth = 100
)
public class PluginAddCommand extends PluginCommand {
    @CommandLine.Parameters(description =
        "A space separated list of plugins specified in the format of <group>:<artifact>:<version>. "
            + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT", arity = "1")
    private List<String> plugins;

    private List<RemoteRepository> repositories;

    @Override
    public List<String> getPlugins() {
        if (plugins == null) {
            plugins = Collections.emptyList();
        }

        return plugins;
    }

    @Override
    protected void executeSubCommand() throws Exception {
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
