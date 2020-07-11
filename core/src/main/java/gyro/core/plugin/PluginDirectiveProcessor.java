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

package gyro.core.plugin;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;
import org.eclipse.aether.resolution.DependencyResult;

@Type("plugin")
public class PluginDirectiveProcessor extends DirectiveProcessor<RootScope> {

    private static final ConcurrentMap<String, Set<Class<?>>> CLASSES_BY_ARTIFACT_COORDS = new ConcurrentHashMap<>();

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        PluginSettings settings = scope.getSettings(PluginSettings.class);
        String artifactCoords = getArgument(scope, node, String.class, 0);
        PluginClassLoader pluginClassLoader = settings.getPluginClassLoader();

        Thread.currentThread().setContextClassLoader(pluginClassLoader);

        settings.addClasses(CLASSES_BY_ARTIFACT_COORDS.computeIfAbsent(artifactCoords, ac -> {
            try {
                DependencyResult result = settings.getDependencyResult(ac);

                try (JarFile jar = new JarFile(result.getRoot().getArtifact().getFile())) {
                    return jar.stream().parallel()
                        // Filter out directories and non-class files.
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().endsWith(".class"))

                        // Map path/filename to Class name.
                        .map(entry -> {
                            String name = entry.getName();

                            name = name.substring(0, name.length() - 6);
                            name = name.replace('/', '.');

                            return name;
                        })

                        // Look up class to for it to load.
                        .map(name -> {
                            try {
                                return Class.forName(name, false, pluginClassLoader);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        })

                        // Ignore abstract classes and interfaces.
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()) && !Modifier.isInterface(c.getModifiers()))
                        .collect(Collectors.toSet());
                }
            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't load the @|bold %s|@ plugin!", ac),
                    error);
            }
        }));
    }
}
