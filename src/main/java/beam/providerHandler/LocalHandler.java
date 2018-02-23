package beam.providerHandler;

import beam.providerBuilder.ProviderBuilder;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class LocalHandler extends ProviderHandler {

    private static String LOCAL_KEY = "^(/[^/ ]*)+/?$";
    private static Pattern LOCAL_KEY_PAT = Pattern.compile(LOCAL_KEY);

    @Override
    public boolean validate(String key) {
        return LOCAL_KEY_PAT.matcher(key).find();
    }

    @Override
    public void handle(String key) {
        File localFile = new File(key);
        try {
            if (isJar(localFile)) {
                loadLibrary(localFile);
            } else if (localFile.isDirectory()) {
                Reflections reflections = new Reflections("beam.providerBuilder");
                for (Class<? extends ProviderBuilder> builderClass : reflections.getSubTypesOf(ProviderBuilder.class)) {
                    try {
                        ProviderBuilder builder = builderClass.newInstance();
                        if (builder.validate(key)) {
                            String packagePath = builder.build(key);
                            loadLibrary(new File(packagePath));
                        }
                    } catch (IllegalAccessException | InstantiationException error) {
                        error.printStackTrace();
                    }
                }
            } else {
                throw new UnsupportedOperationException("Provider needs to be specified by a jar or a source code directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static synchronized void loadLibrary(java.io.File jar) throws Exception {
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
            throw new Exception(e);
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
