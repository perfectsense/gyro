package gyro.core;

import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.util.Bug;
import org.apache.commons.lang3.StringUtils;

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
        String namespace = Optional.ofNullable(c.getAnnotation(Namespace.class))
            .map(Namespace::value)
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> {
                Package pkg = c.getPackage();

                return pkg != null
                    ? NAMESPACES_BY_LOADER.getUnchecked(c.getClassLoader()).getUnchecked(pkg.getName())
                    : "";
            });

        if (namespace.isEmpty()) {
            throw new Bug(String.format(
                "@|bold %s|@ class or one of its packages requires a @Namespace annotation with a non-blank value!",
                c.getName()));
        }

        return namespace;
    }

}
