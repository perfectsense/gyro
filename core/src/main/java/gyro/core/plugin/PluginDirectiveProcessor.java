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
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
                        Class<?> c = Class.forName(name, false, pluginClassLoader);

                        int modifiers = c.getModifiers();

                        if (!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)) {
                            classes.add(c);
                        }
                    }
                }

                return classes;

            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't load the @|bold %s|@ plugin!", ac),
                    error);
            }
        }));
    }

}
