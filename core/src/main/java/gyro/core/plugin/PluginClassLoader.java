package gyro.core.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;

class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader() {
        super(new URL[0], PluginClassLoader.class.getClassLoader());
    }

    public void add(DependencyResult result) throws MalformedURLException {
        for (ArtifactResult r : result.getArtifactResults()) {
            URL url = r.getArtifact().getFile().toURI().toURL();

            if (Stream.of(getURLs()).noneMatch(url::equals)) {
                addURL(url);
            }
        }
    }

}
