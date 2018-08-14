package beam.parser;

import beam.core.*;
import beam.core.diff.*;
import beam.fetcher.ProviderFetcher;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;

public class ASTHandler {

    private static Map<String, String> resourceClassMap = new HashMap<>();

    public static void enterProviderLocation(String key) {

        Reflections reflections = new Reflections("beam.fetcher");
        boolean match = false;
        for (Class<? extends ProviderFetcher> fetcherClass : reflections.getSubTypesOf(ProviderFetcher.class)) {
            try {
                ProviderFetcher fetcher = fetcherClass.newInstance();
                if (fetcher.validate(key)) {
                    fetcher.fetch(key);
                    match = true;
                }
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to access %s", fetcherClass.getName()), error);
            }
        }

        if (!match) {
            throw new BeamException(String.format("Unable to find support for provider: %s", key));
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
        for (Class<? extends BeamObject> resource : reflections.getSubTypesOf(BeamObject.class)) {
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

    public static boolean checkReference(Object value, BeamResource resource, Map<String, BeamResource> symbolTable) {
        boolean hasReference = false;
        if (value instanceof Map) {
            for (Object key : ((Map) value).keySet()) {
                hasReference = checkReference(((Map) value).get(key), resource, symbolTable) || hasReference;
            }

        } else if (value instanceof Collection) {
            for (Object item : (Collection) value) {
                hasReference = checkReference(item, resource, symbolTable) || hasReference;
            }
        } else {
            if (value != null && BeamReference.isReference(value.toString())) {
                if (symbolTable != null) {
                    BeamReference beamReference = new BeamReference(symbolTable, value.toString());
                    BeamResource dependency = beamReference.getResource();
                    resource.dependencies().add(dependency);
                    dependency.dependents().add(resource);
                    resource.getReferences().put(beamReference.getKey(), beamReference);
                }

                hasReference = true;
            } else {
                hasReference = false;
            }
        }

        return hasReference;
    }

    public static void populate(Object object, String key, Object value) {
        try {
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

    // why symbol table has a null key??
    public static void populateSettings(BeamObject object, String key, Object value, Map<String, BeamProvider> providerTable, Map<String, BeamResource> symbolTable) {
        if (object instanceof BeamResource) {
            BeamResource resource = (BeamResource) object;
            if (checkReference(value, resource, symbolTable)) {
                resource.getUnResolvedProperties().put(key, value);
            } else {
                populate(object, key, value);
            }
        } else {
            populate(object, key, value);
        }
    }

    public static BeamObject loadBeamObject(Stack<BeamObject> objectStack, Set<BeamResource> pending) {
        BeamObject object = objectStack.pop();
        if (object instanceof BeamResource) {
            BeamResource resource = (BeamResource) object;
            if (!objectStack.isEmpty() && objectStack.peek() instanceof BeamResource) {
                BeamResource parent = (BeamResource) objectStack.peek();
                parent.getChildren().add(resource);
                resource.setParent(parent);

                parent.dependents().add(resource);
                resource.dependencies().add(parent);

            } else if (objectStack.isEmpty()) {
                pending.add(resource);
            }
        }

        return object;
    }

    public static BeamObject createBeamObject(String providerName, String resourceKey, String id, Map<String, BeamProvider> providerTable, Map<String, BeamResource> symbolTable, Stack<BeamObject> objectStack) {
        String className = null;
        try {
            className = getClassName(providerName, resourceKey);

            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> clazz = loader.loadClass(className);

            BeamObject instance = (BeamObject) clazz.newInstance();
            if (instance instanceof BeamResource) {
                BeamResource resource = (BeamResource) instance;
                if (id != null) {
                    symbolTable.put(id, resource);
                }

            } else if (instance instanceof BeamProvider) {
                BeamProvider provider = (BeamProvider) instance;
                providerTable.put(id, provider);
            }

            objectStack.push(instance);
            return instance;
        } catch (Exception e) {
           throw new BeamException(String.format("Unable to create resource %s::%s", providerName, resourceKey), e);
        }
    }

    private static void adjustCurrentResource(BeamResource currentResource, BeamResource pendingResource, Set<BeamResource> current, Set<BeamResource> adjustCurrent) {
        Iterator<BeamResource> iter = current.iterator();
        BeamResource candidate;
        BeamResource targetResource = null;
        // change to a map will be much faster
        while (iter.hasNext()) {
            candidate = iter.next();
            ResourceDiffKey pendingKey = new ResourceDiffKey(pendingResource.diffIds());
            ResourceDiffKey currentKey = new ResourceDiffKey(candidate.diffIds());
            if (pendingKey.equals(currentKey)) {
                targetResource = candidate;
                iter.remove();
                if (currentResource == null) {
                    adjustCurrent.add(candidate);
                } else {
                    currentResource.getChildren().add(candidate);
                }

                break;
            }
        }

        if (targetResource != null) {
            for (Object child : pendingResource.getChildren()) {
                if (child instanceof BeamResource) {
                    BeamResource childResource = (BeamResource) child;
                    adjustCurrentResource(targetResource, childResource, current, adjustCurrent);
                }
            }
        }
    }

    private static void adjustCurrentConfigs(Set<BeamResource> current, Set<BeamResource> adjustCurrent, Set<BeamResource> pending) {
        for (BeamResource pendingResource : pending) {
            adjustCurrentResource(null, pendingResource, current, adjustCurrent);
        }

        adjustCurrent.addAll(current);
    }

    private static void adjust(BeamResource currentResource, Object currentValue, Map<String, Set<BeamResource>> currentByResourceType, String resourceClass, String resourceProperty) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        if (!ObjectUtils.isBlank(currentValue)) {
            for (String className : currentByResourceType.keySet()) {
                if (className.endsWith(resourceClass)) {
                    Set<BeamResource> resourceSet = currentByResourceType.get(className);
                    for (BeamResource reference : resourceSet) {
                        if (currentValue.equals(DiffUtil.getPropertyValue(reference, resourceClass, resourceProperty))) {
                            currentResource.dependencies().add(reference);
                            reference.dependents().add(currentResource);
                        }
                    }
                }
            }
        }
    }

    private static void adjustCurrentDependencies(Set<BeamResource> current, Map<String, Set<BeamResource>> currentByResourceType) {
        for (BeamResource currentResource : current) {
            try {
                for (PropertyDescriptor p : Introspector.getBeanInfo(currentResource.getClass()).getPropertyDescriptors()) {
                    Method reader = p.getReadMethod();
                    if (reader == null) {
                        continue;
                    }

                    Class<?> returnType = reader.getReturnType();
                    Object currentValue = reader.invoke(currentResource);
                    ResourceReferenceProperty propertyAnnotation = reader.getAnnotation(ResourceReferenceProperty.class);

                    if (propertyAnnotation != null) {
                        String annotation = propertyAnnotation.value();
                        if (StringUtils.isBlank(annotation)) {
                            // throw new BeamException("empty annotation");
                        }

                        String resourceClass = annotation.split("\\.")[0];
                        String resourceProperty = annotation.split("\\.")[1];

                        if (Collection.class.isAssignableFrom(returnType)) {
                            if (currentValue != null) {
                                Collection collection = (Collection) currentValue;
                                Iterator iterator = collection.iterator();
                                while (iterator.hasNext()) {
                                    Object value = iterator.next();
                                    adjust(currentResource, value, currentByResourceType, resourceClass, resourceProperty);
                                }
                            }
                        } else {
                            adjust(currentResource, currentValue, currentByResourceType, resourceClass, resourceProperty);
                        }
                    }
                }
            } catch (IllegalAccessException |
                    IntrospectionException error) {
                throw new IllegalStateException(error);

            } catch (InvocationTargetException error) {
                throw Throwables.propagate(error);
            }
        }
    }

    private void tryToKeep(List<Diff<?, ?, ?>> diffs) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                if (change instanceof ResourceUpdate) {
                    ((ResourceUpdate) change).tryToKeep();
                }

                tryToKeep(change.getDiffs());
            }
        }
    }

    private static void resolveReference(List<Diff<?, ?, ?>> diffs) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                if (change instanceof ResourceChange) {
                    ((ResourceChange) change).resolveReference();
                }

                resolveReference(change.getDiffs());
            }
        }
    }

    public static ResourceDiff exitBeamRoot(Set<BeamResource> pending, Set<ChangeType> changeTypes, Map<String, BeamProvider> providerTable) {
        BeamProvider beamProvider = null;
        for (BeamProvider oneProvider : providerTable.values()) {
            beamProvider = oneProvider;
        }

        Reflections reflections = new Reflections("beam");
        Set<BeamResource> current = new HashSet<>();

        Map<String, Set<BeamResource>> currentByResourceType = new HashMap<>();
        Set<BeamResource> adjustCurrent = new HashSet<>();

        long startTime = System.currentTimeMillis();
        for (Class<? extends BeamResource> resourceClass : reflections.getSubTypesOf(BeamResource.class)) {
            try {
                ConfigKey configKey = resourceClass.getAnnotation(ConfigKey.class);
                if (configKey == null) {
                    continue;
                }

                BeamResource resource = resourceClass.newInstance();
                Set<BeamResource> resourceSet = new HashSet<>();
                resource.init(beamProvider, null, resourceSet);
                current.addAll(resourceSet);
                currentByResourceType.put(resourceClass.getName(), resourceSet);
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to load resource from %s", resourceClass), error);
            }
        }

        long endTime = System.currentTimeMillis();

        System.out.println("load current took " + (endTime - startTime)/1000.0 + " seconds");

        startTime = System.currentTimeMillis();
        adjustCurrentDependencies(current, currentByResourceType);
        adjustCurrentConfigs(current, adjustCurrent, pending);
        endTime = System.currentTimeMillis();

        System.out.println("adjust current took " + (endTime - startTime)/1000.0 + " seconds");

        try {
            ResourceDiff resourceDiff = DiffUtil.generateDiff(beamProvider, null, adjustCurrent, pending);
            resolveReference(Arrays.asList(resourceDiff));
            return resourceDiff;

        } catch (Exception e) {
            throw new BeamException(e.getMessage(), e.getCause());
        }
    }
}
