package beam.core.diff;

import beam.Beam;
import beam.core.BeamReference;
import beam.core.BeamResource;
import beam.core.BeamProvider;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    // this is to resolve references for diff
    // also need to resolve reference when create
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
                Method reader = p.getReadMethod();
                if (reader == null) {
                    continue;
                }

                Class<?> returnType = reader.getReturnType();
                Object pendingValue = reader.invoke(resource);

                ResourceReferenceProperty propertyAnnotation = reader.getAnnotation(ResourceReferenceProperty.class);

                if (propertyAnnotation != null) {
                    String annotation = propertyAnnotation.value();
                    if (StringUtils.isBlank(annotation)) {
                        //throw new BeamException("empty annotation");
                    }

                    String resourceClass = annotation.split("\\.")[0];
                    String resourceProperty = annotation.split("\\.")[1];

                    if (!resource.getReferences().containsKey(p.getName())) {
                        if (ObjectUtils.isBlank(pendingValue)) {
                            if (resource.getParent() != null) {
                                BeamResource parent = resource.getParent();
                                Object value = DiffUtil.getPropertyValue(parent, resourceClass, resourceProperty);
                                Method writeMethod = p.getWriteMethod();
                                writeMethod.invoke(resource, value);
                            }
                        }

                        continue;
                    }

                    Object referenceMeta = resource.getReferences().get(p.getName());
                    boolean resolvedAll = true;
                    if (Collection.class.isAssignableFrom(returnType)) {
                        Collection collection = (Collection) referenceMeta;
                        Iterator iterator = collection.iterator();
                        Set references = new HashSet();
                        while (iterator.hasNext()) {
                            Object value = iterator.next();
                            iterator.remove();
                            if (value != null && value instanceof BeamReference) {
                                BeamReference beamReference = (BeamReference) value;
                                Object referenceValue = beamReference.resolveReference();
                                if (referenceValue == null) {
                                    references.add(value);
                                    resolvedAll = false;
                                } else {
                                    references.add(referenceValue);
                                }
                            } else {
                                references.add(value);
                            }
                        }

                        collection.addAll(references);

                    } else {
                        if (referenceMeta instanceof BeamReference) {
                            BeamReference beamReference = (BeamReference) referenceMeta;
                            Object referenceValue = beamReference.resolveReference();
                            if (referenceValue != null) {
                                Method writer = p.getWriteMethod();
                                writer.invoke(resource, referenceValue);
                            } else {
                                resolvedAll = false;
                            }
                        }
                    }

                    if (resolvedAll) {
                        resource.getReferences().remove(p.getName());
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
