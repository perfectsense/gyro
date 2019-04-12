package gyro.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public PluginClassLoader(URL[] urls) {
        super(urls);
    }

    public PluginClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public void addAllUrls(URL[] urls) {
        for (URL url : urls) {
            addURL(url);
        }
    }

}
