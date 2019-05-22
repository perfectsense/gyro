package gyro.core;

import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class NamespaceUtils {

    private static final LoadingCache<ClassLoader, LoadingCache<String, String>> NAMESPACES_BY_LOADER = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new CacheLoader<ClassLoader, LoadingCache<String, String>>() {

            @Override
            public LoadingCache<String, String> load(ClassLoader loader) {
                return CacheBuilder.newBuilder()
                    .build(new CacheLoader<String, String>() {

                        @Override
                        public String load(String name) {
                            Package pkg;

                            try {
                                pkg = Class.forName(name + ".package-info", true, loader).getPackage();

                            } catch (ClassNotFoundException error) {
                                pkg = null;
                            }

                            return Optional.ofNullable(pkg)
                                .map(p -> p.getAnnotation(Namespace.class))
                                .map(Namespace::value)
                                .orElseGet(() -> {
                                    int lastDotAt = name.lastIndexOf('.');

                                    return lastDotAt > -1
                                        ? NAMESPACES_BY_LOADER.getUnchecked(loader).getUnchecked(name.substring(0, lastDotAt))
                                        : "";
                                });
                        }
                    });
            }
        });

    public static String getNamespace(Class<?> c) {
        return Optional.ofNullable(c.getAnnotation(Namespace.class))
            .map(Namespace::value)
            .orElseGet(() -> {
                Package pkg = c.getPackage();

                return pkg != null
                    ? NAMESPACES_BY_LOADER.getUnchecked(c.getClassLoader()).getUnchecked(pkg.getName())
                    : "";
            });
    }

    public static String getNamespacePrefix(Class<?> c) {
        String namespace = getNamespace(c);

        return namespace.isEmpty() ? "" : namespace + "::";
    }

}
