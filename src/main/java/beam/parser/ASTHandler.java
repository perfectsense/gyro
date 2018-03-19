package beam.parser;

import beam.core.*;
import beam.core.diff.*;
import beam.providerFetcher.ProviderFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import org.fusesource.jansi.AnsiRenderWriter;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class ASTHandler {

    public static void enterProviderLocation(String key) {
        Reflections reflections = new Reflections("beam.providerFetcher");
        for (Class<? extends ProviderFetcher> handlerClass : reflections.getSubTypesOf(ProviderFetcher.class)) {
            try {
                ProviderFetcher handler = handlerClass.newInstance();
                if (handler.validate(key)) {
                    handler.fetch(key);
                }
            } catch (IllegalAccessException | InstantiationException error) {
                error.printStackTrace();
            }
        }
    }

    private static String getClassName(String packageName, String resourceKey) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName)), ClasspathHelper.forPackage("beam.core"));

        String className = null;
        for (Class<? extends BeamObject> resource : reflections.getSubTypesOf(BeamObject.class)) {
            ConfigKey configKey = resource.getAnnotation(ConfigKey.class);
            if (configKey != null && resourceKey.equals(configKey.value())) {
                className = resource.getName();
            }
        }

        return className;
    }

    public static void populateSettings(BeamObject object, String key, String value, Map<String, BeamProvider> providerTable, Map<String, BeamResource> symbolTable) {
        // need to convert value to the correct type
        // need to remove reference and reference calls in BeamResource
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (object instanceof BeamResource) {
                BeamResource resource = (BeamResource) object;
                PropertyDescriptor pd = new PropertyDescriptor(key, object.getClass());
                Method setter = pd.getWriteMethod();

                for (Class type : setter.getParameterTypes()) {
                    if (Collection.class.isAssignableFrom(type)) {
                        boolean needReference = false;
                        Collection collection = mapper.readValue(value, Collection.class);
                        List referenceList = new ArrayList();
                        Iterator iterator = collection.iterator();
                        while (iterator.hasNext()) {
                            Object item = iterator.next();
                            if (item != null && BeamReference.isReference(item.toString())) {
                                BeamReference beamReference = new BeamReference(symbolTable, item.toString());
                                BeamResource dependency = beamReference.getResource();
                                resource.dependencies().add(dependency);
                                dependency.dependents().add(resource);
                                referenceList.add(beamReference);
                                needReference = true;
                            } else if (item != null) {
                                referenceList.add(item);
                            }
                        }

                        if (!needReference) {
                            setter.invoke(object, collection);
                        } else {
                            resource.getReferences().put(key, referenceList);
                        }
                    } else {
                        if (BeamReference.isReference(value)) {
                            BeamReference beamReference = new BeamReference(symbolTable, value);
                            BeamResource dependency = beamReference.getResource();
                            resource.dependencies().add(dependency);
                            dependency.dependents().add(resource);
                            resource.getReferences().put(key, beamReference);
                        } else {
                            setter.invoke(object, mapper.readValue(value, type));
                        }
                    }
                }
            } else {
                PropertyDescriptor pd = new PropertyDescriptor(key, object.getClass());
                Method setter = pd.getWriteMethod();
                for (Class type : setter.getParameterTypes()) {
                    setter.invoke(object, mapper.readValue(value, type));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            String packageName = String.format("beam.%s", providerName);
            className = getClassName(packageName, resourceKey);
            if (className == null) {
                throw new UnsupportedOperationException(String.format("%s::%s is not supported!", providerName, resourceKey));
            }

            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> clazz = loader.loadClass(className);

            BeamObject instance = (BeamObject) clazz.newInstance();
            if (instance instanceof BeamResource) {
                BeamResource resource = (BeamResource) instance;
                symbolTable.put(id, resource);

            } else if (instance instanceof BeamProvider) {
                BeamProvider provider = (BeamProvider) instance;
                providerTable.put(id, provider);
            }

            objectStack.push(instance);
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

    public static void exitBeamRoot(Set<BeamResource> pending, Set<ChangeType> changeTypes, Map<String, BeamProvider> providerTable) {
        PrintWriter out = new AnsiRenderWriter(System.out, true);
        BeamProvider beamProvider = null;
        for (BeamProvider oneProvider : providerTable.values()) {
            beamProvider = oneProvider;
        }

        Reflections reflections = new Reflections("beam");
        Set<BeamResource> current = new HashSet<>();

        // use class as key
        Map<String, Set<BeamResource>> currentByResourceType = new HashMap<>();
        Set<BeamResource> adjustCurrent = new HashSet<>();
        for (Class<? extends BeamResource> resourceClass : reflections.getSubTypesOf(BeamResource.class)) {
            try {
                BeamResource resource = resourceClass.newInstance();
                Set<BeamResource> resourceSet = new HashSet<>();
                resource.init(beamProvider, null, resourceSet);
                current.addAll(resourceSet);
                // use class name
                currentByResourceType.put(resourceClass.getName(), resourceSet);
            } catch (IllegalAccessException | InstantiationException error) {
                error.printStackTrace();
            }
        }

        adjustCurrentDependencies(current, currentByResourceType);
        adjustCurrentConfigs(current, adjustCurrent, pending);
        ResourceDiff resourceDiff = new ResourceDiff(
                beamProvider,
                null,
                adjustCurrent,
                pending);

        BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            resourceDiff.diff();
            resourceDiff.getChanges().clear();
            resourceDiff.diff();

            resolveReference(Arrays.asList(resourceDiff));

            changeTypes.clear();
            DiffUtil.writeDiffs(Arrays.asList(resourceDiff), 0, out, changeTypes);

            boolean hasChanges = false;
            if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {

                out.format("\nAre you sure you want to create and/or update resources? (y/N) ");
                out.flush();
                hasChanges = true;

                if ("y".equalsIgnoreCase(confirmReader.readLine())) {
                    out.println("");
                    out.flush();
                    DiffUtil.setChangeable(Arrays.asList(resourceDiff));
                    DiffUtil.createOrUpdate(Arrays.asList(resourceDiff), out);
                }
            }

            boolean delete = true;
            if (changeTypes.contains(ChangeType.DELETE)) {
                hasChanges = true;

                if (delete) {
                    out.format("\nAre you sure you want to delete resources? (y/N) ");
                    out.flush();

                    if ("y".equalsIgnoreCase(confirmReader.readLine())) {

                        out.println("");
                        out.flush();
                        DiffUtil.setChangeable(Arrays.asList(resourceDiff));
                        DiffUtil.delete(Arrays.asList(resourceDiff), out);
                    }

                } else {
                    out.format("\nSkipped deletes. Run again with the --delete option to execute them.\n");
                    out.flush();
                }
            }

            if (!hasChanges) {
                out.format("\nNo changes.\n");
                out.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
