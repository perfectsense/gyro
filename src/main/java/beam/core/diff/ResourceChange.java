package beam.core.diff;

import beam.core.BeamException;
import beam.core.BeamReference;
import beam.core.BeamResource;
import beam.core.BeamProvider;
import beam.parser.ASTHandler;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class ResourceChange<B extends BeamProvider> extends Change<BeamResource<B>> {

    private final ResourceDiff diff;
    private final BeamResource<B> resource;

    public ResourceChange(ResourceDiff diff, ChangeType type, BeamResource<B> current, BeamResource<B> pending) {
        super(type, current);

        this.diff = diff;
        this.resource = pending != null ? pending : current;
    }

    public BeamResource<B> getResource() {
        return resource;
    }

    public void create(Collection<? extends BeamResource<B>> pendingResources) throws Exception {
        diff.create(this, pendingResources);
    }

    public void createOne(BeamResource<B> pendingResource) throws Exception {
        diff.createOne(this, pendingResource);
    }

    public <R extends BeamResource<B>> void update(Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        diff.update(this, currentResources, pendingResources);
    }

    public <R extends BeamResource<B>> void updateOne(R currentResource, R pendingResource) throws Exception {
        diff.updateOne(this, currentResource, pendingResource);
    }

    public void delete(Collection<? extends BeamResource<B>> pendingResources) throws Exception {
        diff.delete(this, pendingResources);
    }

    public void deleteOne(BeamResource<B> pendingResource) throws Exception {
        diff.deleteOne(this, pendingResource);
    }

    @Override
    public Set<Change<?>> dependencies() {
        Set<Change<?>> dependencies = new HashSet<>();

        for (BeamResource<B> r : (getType() == ChangeType.DELETE ?
                resource.dependents() :
                resource.dependencies())) {

            Change<?> c = r.getChange();

            if (c != null) {
                dependencies.add(c);
            }
        }

        return dependencies;
    }

    private Object resolveReference(Object value, BeamResource resource) {
        Object result;
        if (value instanceof Map) {
            result = new HashMap<>();
            for (Object key : ((Map) value).keySet()) {
                ((Map) result).put(key, resolveReference(((Map) value).get(key), resource));
            }

        } else if (value instanceof Collection) {
            result = new ArrayList<>();
            for (Object item : (Collection) value) {
                ((Collection) result).add(resolveReference(item, resource));
            }
        } else {
            if (value != null && BeamReference.isReference(value.toString())) {
                BeamReference beamReference = (BeamReference) resource.getReferences().get(value.toString());
                result = beamReference.resolveReference();
                if (result == null) {
                    result = value;
                } else {
                    resource.getReferences().remove(value.toString());
                }
            } else {
                result = value;
            }
        }

        return result;
    }

    public void resolveReference() {
        try {
            Class klass = resource != null ? resource.getClass() : null;
            if (klass == null && getCurrentAsset() != null) {
                klass = getCurrentAsset().getClass();
            }

            if (klass == null) {
                return;
            }

            for (PropertyDescriptor p : Introspector.getBeanInfo(klass).getPropertyDescriptors()) {
                String key = p.getName();
                Method reader = p.getReadMethod();
                if (reader == null) {
                    continue;
                }

                Object pendingValue = reader.invoke(resource);
                ResourceReferenceProperty propertyAnnotation = reader.getAnnotation(ResourceReferenceProperty.class);

                if (propertyAnnotation != null) {
                    String annotation = propertyAnnotation.value();
                    if (StringUtils.isBlank(annotation)) {
                        throw new BeamException(String.format("Invalid annotation for %s in %s", key, klass.getName()));
                    }

                    String resourceClass = annotation.split("\\.")[0];
                    String resourceProperty = annotation.split("\\.")[1];

                    // parent association need some improvement: e.g. security group ip permission have groupId and fromGroup, both are candidates for the parent ID
                    if (!resource.getUnResolvedProperties().containsKey(p.getName())) {
                        if (ObjectUtils.isBlank(pendingValue)) {
                            if (resource.getParent() != null) {
                                BeamResource parent = resource.getParent();
                                Object value = DiffUtil.getPropertyValue(parent, resourceClass, resourceProperty);
                                if (value != null) {
                                    Method writeMethod = p.getWriteMethod();
                                    writeMethod.invoke(resource, value);
                                }
                            }
                        }

                        continue;
                    }

                    Object value = resource.getUnResolvedProperties().get(p.getName());
                    Object result = resolveReference(value, resource);
                    if (ASTHandler.checkReference(result, resource, null)) {
                        resource.getUnResolvedProperties().put(p.getName(), result);
                    } else {
                        ASTHandler.populate(resource, p.getName(), result);
                        resource.getUnResolvedProperties().remove(p.getName());
                    }
                }
            }
        } catch (IllegalAccessException |
                IntrospectionException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        } catch (Exception e) {
            throw new BeamException("Unable to resolve reference", e);
        }
    }
}
