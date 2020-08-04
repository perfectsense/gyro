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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.RootScope;
import gyro.core.scope.Settings;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.DependencyResult;

public class PluginSettings extends Settings {

    private static final ConcurrentMap<String, VersionUrl> VERSIONED_URLS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, DependencyResult> RESULT_BY_ARTIFACT_COORDS = new ConcurrentHashMap<>();
    private static final PluginClassLoader PLUGIN_CLASS_LOADER = new PluginClassLoader();

    private List<Plugin> plugins;
    private List<Class<?>> otherClasses;
    private Path cachePath;

    private final LoadingCache<Plugin, LoadingCache<Class<?>, Boolean>> call = CacheBuilder.newBuilder()
        .build(new CacheLoader<Plugin, LoadingCache<Class<?>, Boolean>>() {

            @Override
            public LoadingCache<Class<?>, Boolean> load(Plugin plugin) {
                return CacheBuilder.newBuilder()
                    .build(new CacheLoader<Class<?>, Boolean>() {

                        @Override
                        public Boolean load(Class<?> otherClass) throws Exception {
                            plugin.onEachClass((RootScope) getScope(), otherClass);
                            return Boolean.TRUE;
                        }
                    });
            }
        });

    public List<Plugin> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<>();
        }

        return plugins;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    public List<Class<?>> getOtherClasses() {
        if (otherClasses == null) {
            otherClasses = new ArrayList<>();
        }

        return otherClasses;
    }

    public void setOtherClasses(List<Class<?>> otherClasses) {
        this.otherClasses = otherClasses;
    }

    public void addClasses(Set<Class<?>> classes) {
        List<Plugin> plugins = getPlugins();
        List<Class<?>> otherClasses = getOtherClasses();

        for (Class<?> c : classes) {
            if (Plugin.class.isAssignableFrom(c)) {
                plugins.add((Plugin) Reflections.newInstance(c));

            } else {
                otherClasses.add(c);
            }
        }

        for (Plugin plugin : plugins) {
            otherClasses.stream().parallel()
                .forEach(otherClass -> {
                    try {
                        call.get(plugin).get(otherClass);

                    } catch (ExecutionException error) {
                        throw new GyroException(
                            String.format(
                                "Can't load @|bold %s|@ using the @|bold %s|@ plugin!",
                                otherClass.getName(),
                                plugin.getClass().getName()),
                            error.getCause());
                    }
                });
        }
    }

    public Path getCachePath() {
        return cachePath;
    }

    public void setCachePath(Path cachePath) {
        this.cachePath = cachePath;
    }

    public PluginClassLoader getPluginClassLoader() {
        return PLUGIN_CLASS_LOADER;
    }

    public DependencyResult getDependencyResult(String ac) {
        return RESULT_BY_ARTIFACT_COORDS.get(ac);
    }

    public void putDependencyResult(String ac, DependencyResult result) {
        RESULT_BY_ARTIFACT_COORDS.put(ac, result);
    }

    public boolean pluginInitialized(String artifactCoord) {
        return RESULT_BY_ARTIFACT_COORDS.containsKey(artifactCoord);
    }

    public void putArtifactIfNewer(Artifact artifact) throws MalformedURLException {
        String id = artifact.getGroupId() + "/" + artifact.getArtifactId();
        VersionUrl versionUrl = new VersionUrl(artifact.getVersion(), artifact.getFile().toURI().toURL());

        if (!VERSIONED_URLS.containsKey(id) || versionUrl.compareTo(VERSIONED_URLS.get(id)) > 0) {
            VERSIONED_URLS.put(id, versionUrl);
        }
    }

    public void addAllUrls() {
        PLUGIN_CLASS_LOADER.add(VERSIONED_URLS.values().stream().map(VersionUrl::getUrl).collect(Collectors.toList()));
    }

    private static class VersionUrl implements Comparable<VersionUrl> {

        private String version;
        private URL url;

        public String getVersion() {
            return version;
        }

        public URL getUrl() {
            return url;
        }

        public VersionUrl(String version, URL url) {
            this.version = version;
            this.url = url;
        }

        @Override
        public int compareTo(VersionUrl other) {
            return new ComparableVersion(this.getVersion()).compareTo(new ComparableVersion(other.getVersion()));
        }
    }
}
