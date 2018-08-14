package beam.fetcher;

import beam.core.BeamException;
import beam.builder.ProviderBuilder;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class LocalFetcher extends ProviderFetcher {

    private static String LOCAL_KEY = "^(/[^/ ]*)+/?$";
    private static Pattern LOCAL_KEY_PAT = Pattern.compile(LOCAL_KEY);

    @Override
    public boolean validate(String key) {
        return LOCAL_KEY_PAT.matcher(key).find();
    }

    @Override
    public void fetch(String key) {
        File localFile = new File(key);
        try {
            if (isJar(localFile)) {
                loadLibrary(localFile);
            } else if (localFile.isDirectory()) {
                Reflections reflections = new Reflections("beam.builder");
                boolean match = false;
                for (Class<? extends ProviderBuilder> builderClass : reflections.getSubTypesOf(ProviderBuilder.class)) {
                    try {
                        ProviderBuilder builder = builderClass.newInstance();
                        if (builder.validate(key)) {
                            String packagePath = builder.build(key);
                            loadLibrary(new File(packagePath));
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
                throw new UnsupportedOperationException("Provider needs to be specified by a jar or a source code directory");
            }
        } catch (IOException ioe) {
            throw new BeamException(ioe.getMessage(), ioe);
        }
    }

    protected static synchronized void loadLibrary(java.io.File jar) {
        try {
            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            java.net.URL url = jar.toURI().toURL();
            for (java.net.URL it : java.util.Arrays.asList(loader.getURLs())) {
                if (it.equals(url)){
                    return;
                }
            }

            java.lang.reflect.Method method = java.net.URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{java.net.URL.class});
            method.setAccessible(true);
            method.invoke(loader, new Object[]{url});
        } catch (java.lang.NoSuchMethodException |
                java.lang.IllegalAccessException |
                java.net.MalformedURLException |
                java.lang.reflect.InvocationTargetException e) {
            throw new BeamException(String.format("Unable to load provider library: %s", jar.getAbsolutePath()), e);
        }
    }

    protected static boolean isJar(File file) throws IOException {
        if (!isZip(file)) {
            return false;
        }

        ZipFile zip = new ZipFile(file);
        boolean manifest = zip.getEntry("META-INF/MANIFEST.MF") != null;
        zip.close();
        return manifest;
    }

    private static boolean isZip(File file) throws IOException {
        if (file.isDirectory()) {
            return false;
        }

        if (!file.canRead()) {
            throw new IOException("Unable to read file " + file.getAbsolutePath());
        }

        if (file.length() < 4) {
            return false;
        }

        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        int test = in.readInt();
        in.close();
        return test == 0x504b0304;
    }
}
