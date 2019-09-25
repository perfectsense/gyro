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

package gyro.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.util.Bug;
import org.apache.commons.lang3.StringUtils;

public class Reflections {

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

    public static Optional<String> getNamespaceOptional(Class<?> aClass) {
        String namespace = Optional.ofNullable(aClass.getAnnotation(Namespace.class))
            .map(Namespace::value)
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> {
                Package pkg = aClass.getPackage();

                return pkg != null
                    ? NAMESPACES_BY_LOADER.getUnchecked(aClass.getClassLoader()).getUnchecked(pkg.getName())
                    : "";
            });

        return namespace.isEmpty() ? Optional.empty() : Optional.of(namespace);
    }

    public static String getNamespace(Class<?> aClass) {
        return getNamespaceOptional(aClass).orElseThrow(() -> new Bug(String.format(
            "@|bold %s|@ class or one of its packages requires a @Namespace annotation with a non-blank value!",
            aClass.getName())));
    }

    public static Optional<String> getTypeOptional(Class<?> aClass) {
        return Optional.ofNullable(aClass.getAnnotation(Type.class))
            .map(Type::value)
            .filter(StringUtils::isNotBlank);
    }

    public static String getType(Class<?> aClass) {
        return getTypeOptional(aClass).orElseThrow(() -> new Bug(String.format(
            "@|bold %s|@ class requires a @Type annotation with a non-blank value!",
            aClass.getName())));
    }

    public static BeanInfo getBeanInfo(Class<?> aClass) {
        try {
            return Introspector.getBeanInfo(aClass);

        } catch (IntrospectionException error) {
            throw new GyroException(
                String.format("Can't introspect @|bold %s|@!", aClass.getName()),
                error);
        }
    }

    public static <T> T newInstance(Class<T> aClass) {
        try {
            return aClass.newInstance();

        } catch (IllegalAccessException | InstantiationException error) {
            throw new Bug(error);
        }
    }

    public static Object invoke(Method method, Object object, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(object, arguments);

        } catch (IllegalAccessException error) {
            throw new Bug(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new GyroException(cause);
        }
    }

}
