package beam.parser;

import beam.core.*;
import beam.fetcher.PluginFetcher;
import beam.lang.BeamConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psddev.dari.util.ObjectUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import com.google.common.base.CaseFormat;

public class ASTHandler {

    private static Map<String, String> resourceClassMap = new HashMap<>();

    public static void fetchPlugin(String path) {
        Reflections reflections = new Reflections("beam.fetcher");
        boolean match = false;
        for (Class<? extends PluginFetcher> fetcherClass : reflections.getSubTypesOf(PluginFetcher.class)) {
            try {
                PluginFetcher fetcher = fetcherClass.newInstance();
                if (fetcher.validate(path)) {
                    fetcher.fetch(path);
                    match = true;
                }
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to access %s", fetcherClass.getName()), error);
            }
        }

        if (!match) {
            throw new BeamException(String.format("Unable to find support for plugin: %s", path));
        }
    }

    private static String getClassName(String providerName, String resourceKey) {
        String key = String.format("%s::%s", providerName, resourceKey);
        if (resourceClassMap.containsKey(key)) {
            return resourceClassMap.get(key);
        }

        String packageName = String.format("beam.%s", providerName);
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName)), ClasspathHelper.forPackage("beam.core"));

        String className = null;
        for (Class<? extends BeamConfig> resource : reflections.getSubTypesOf(BeamConfig.class)) {
            ConfigKey configKey = resource.getAnnotation(ConfigKey.class);
            if (configKey != null && resourceKey.equals(configKey.value())) {
                className = resource.getName();
            }
        }

        if (className == null) {
            throw new BeamException(String.format("Unsupported resource %s::%s", providerName, resourceKey));
        }

        resourceClassMap.put(key, className);
        return className;
    }

    public static void populate(Object object, String key, Object value) {
        try {
            key = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
            ObjectMapper mapper = new ObjectMapper();
            PropertyDescriptor pd = new PropertyDescriptor(key, object.getClass());
            Method setter = pd.getWriteMethod();

            if (setter == null) {
                throw new BeamException(String.format("Unable find setter for %s in %s", key, object.getClass()));
            } else if (setter.getParameterTypes().length != 1) {
                throw new BeamException(String.format("Invalid setter for field %s in %s: setter accepts more than 1 argument", key, object.getClass()));
            }

            Class parameterType = setter.getParameterTypes()[0];
            Type[] types = setter.getGenericParameterTypes();

            if (Map.class.isAssignableFrom(parameterType)) {
                ParameterizedType pType = (ParameterizedType) types[0];
                Class<?> keyClass = (Class<?>) pType.getActualTypeArguments()[0];
                Class<?> valueClass = (Class<?>) pType.getActualTypeArguments()[1];
                JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
                setter.invoke(object, (Object) mapper.readValue(ObjectUtils.toJson(value), mapType));
            } else if (Collection.class.isAssignableFrom(parameterType)) {
                ParameterizedType pType = (ParameterizedType) types[0];
                Class<?> valueClass = (Class<?>) pType.getActualTypeArguments()[0];
                JavaType collectionType = mapper.getTypeFactory().constructCollectionType(parameterType, valueClass);
                // throw an error if the collection is null
                setter.invoke(object, (Object) mapper.readValue(ObjectUtils.toJson(value), collectionType));
            } else {
                JavaType valueType = mapper.getTypeFactory().constructType(parameterType);
                setter.invoke(object, (Object) mapper.readValue(ObjectUtils.toJson(value), valueType));
            }

        } catch (Exception e) {
            throw new BeamException(String.format("Unable to populate %s with %s in %s!", key, value, object), e);
        }
    }
}
