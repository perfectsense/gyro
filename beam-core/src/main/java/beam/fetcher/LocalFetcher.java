package beam.fetcher;

import beam.core.BeamException;
import beam.builder.ProviderBuilder;
import beam.lang.BeamConfig;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class LocalFetcher extends PluginFetcher {

    private static String LOCAL_KEY = "^(/[^/ ]*)+/?$";
    private static Pattern LOCAL_KEY_PAT = Pattern.compile(LOCAL_KEY);

    @Override
    public boolean validate(BeamConfig fetcherContext) {
        if (fetcherContext.get("path") != null) {
            String key = (String) fetcherContext.get("path").getValue();
            if (key != null) {
                File path = new File(key);
                return path.exists();
            }
        }

        return false;
    }

    @Override
    public void fetch(BeamConfig fetcherContext) {
        String key = (String) fetcherContext.get("path").getValue();
        File localFile = new File(key);
        try {
           if (localFile.isDirectory()) {
                Reflections reflections = new Reflections("beam.builder");
                boolean match = false;
                for (Class<? extends ProviderBuilder> builderClass : reflections.getSubTypesOf(ProviderBuilder.class)) {
                    try {
                        ProviderBuilder builder = builderClass.newInstance();
                        if (builder.validate(key)) {
                            String packagePath = builder.build(key);

                            URL path[] = { new URL("file:///" + packagePath) };
                            ClassLoader parent = ClassLoader.getSystemClassLoader();
                            URLClassLoader loader = new URLClassLoader(path, parent);

                            match = true;
                        }
                    } catch (IllegalAccessException | InstantiationException error) {
                        throw new BeamException(String.format("Unable to access %s", builderClass.getName()), error);
                    }
                }

                if (!match) {
                    throw new BeamException(String.format("Unable to find builder for provider: %s", key));
                }
            } else {
                throw new UnsupportedOperationException("Provider needs to be specified a source code directory");
            }
        } catch (IOException ioe) {
            throw new BeamException(ioe.getMessage(), ioe);
        }
    }

}
