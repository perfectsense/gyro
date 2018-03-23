package beam.core.diff;

import beam.core.*;
import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ResourceDiff<B extends BeamProvider> extends Diff<ResourceDiffKey, BeamResource<B>, BeamResource<B>> {

    private final B cloud;
    private final BeamResourceFilter filter;
    private final Iterable<? extends BeamResource<B>> currentResources;
    private final Iterable<? extends BeamResource<B>> pendingResources;

    public ResourceDiff(
            B cloud,
            BeamResourceFilter filter,
            Iterable<? extends BeamResource<B>> currentResources,
            Iterable<? extends BeamResource<B>> pendingResources) {

        this.cloud = cloud;
        this.filter = filter;

        List<BeamResource<B>> crs = new ArrayList<>();

        if (currentResources != null) {
            for (BeamResource<B> cr : currentResources) {
                if (filter == null || filter.isInclude(cr)) {
                    crs.add(cr);
                }
            }
        }

        this.currentResources = crs;
        this.pendingResources = pendingResources != null ? pendingResources : Collections.<BeamResource<B>>emptySet();
    }

    @Override
    public Iterable<? extends BeamResource<B>> getCurrentAssets() {
        return currentResources;
    }

    @Override
    public ResourceDiffKey getIdFromAsset(BeamResource<B> asset) {
        return new ResourceDiffKey(asset.diffIds());
    }

    @Override
    public Iterable<? extends BeamResource<B>> getPendingConfigs() {
        return pendingResources;
    }

    @Override
    public ResourceDiffKey getIdFromConfig(BeamResource<B> config) {
        return new ResourceDiffKey(config.diffIds());
    }

    @Override
    public ResourceChange newCreate(final BeamResource<B> pending) throws Exception {
        BeamResource<B> current = pending.findCurrent(cloud, filter);

        if (current != null) {
            return newUpdate(current, pending);
        }

        ResourceChange create = new ResourceChange(this, ChangeType.CREATE, null, pending) {

            @Override
            protected BeamResource<B> change() {
                this.resolveReference();
                try {
                    for (PropertyDescriptor p : Introspector.getBeanInfo(pending.getClass()).getPropertyDescriptors()) {
                        Method reader = p.getReadMethod();
                        if (reader == null) {
                            continue;
                        }

                        DiffId diffId = reader.getAnnotation(DiffId.class);
                        if (diffId != null) {
                            Object pendingValue = reader.invoke(pending);
                            if (pendingValue == null && pending.getConfigLocation() != null) {
                                pending.getConfigLocation().getContentMap().put(p.getName(), null);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                pending.create(cloud);

                try {
                    if (pending.getConfigLocation() != null) {
                        Map<String, String> contentMap = pending.getConfigLocation().getContentMap();
                        for (String property : contentMap.keySet()) {
                            Object value = DiffUtil.getPropertyValue(pending, null, property);
                            if (value != null) {
                                contentMap.put(property, value.toString());
                            } else {
                                contentMap.remove(property);
                            }
                        }

                        BeamRuntime.getBeamConfigLocations().add(pending.getConfigLocation());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return pending;
            }

            @Override
            public String toString() {
                return String.format(
                        "Create %s",
                        pending.toDisplayString());
            }
        };

        pending.setChange(create);
        pending.diffOnCreate(create);
        return create;
    }

    @Override
    public ResourceChange newUpdate(final BeamResource<B> current, final BeamResource<B> pending) throws Exception {

        // Fill the empty properties in pending with the values from current.
        try {
            Class klass = pending != null ? pending.getClass() : null;
            if (klass == null && current != null) {
                klass = current.getClass();
            }

            if (klass != null) {
                for (PropertyDescriptor p : Introspector.getBeanInfo(klass).getPropertyDescriptors()) {
                    Method reader = p.getReadMethod();

                    if (reader != null) {
                        Method writer = p.getWriteMethod();

                        Object pendingValue = reader.invoke(pending);
                        if (writer != null && !isNullable(reader) &&
                                (pendingValue == null ||
                                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullArrayList) ||
                                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullSet))) {
                            writer.invoke(pending, reader.invoke(current));
                        }
                    }
                }
            }

        } catch (IllegalAccessException |
                IntrospectionException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        ResourceUpdate update = new ResourceUpdate(this, current, pending, cloud);

        current.setChange(update);
        pending.setChange(update);
        pending.diffOnUpdate(update, current);
        return update;
    }

    @Override
    public ResourceChange newDelete(final BeamResource<B> current) throws Exception {
        if (!current.isDeletable()) {
            return null;
        }

        ResourceChange delete = new ResourceChange(this, ChangeType.DELETE, current, null) {

            @Override
            protected BeamResource<B> change() {
                current.delete(cloud);
                return null;
            }

            @Override
            public String toString() {
                return String.format(
                        "Delete %s",
                        current.toDisplayString());
            }
        };

        current.setChange(delete);
        current.diffOnDelete(delete);
        return delete;
    }

    public void create(Change<?> change, Collection<? extends BeamResource<B>> pendingResources) throws Exception {

        ResourceDiff diff = new ResourceDiff(
                cloud,
                filter,
                null,
                pendingResources);

        diff.diff();
        change.getDiffs().add(diff);
    }

    public void createOne(Change<?> change, BeamResource<B> pendingResource) throws Exception {
        if (pendingResource != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    null,
                    Arrays.asList(pendingResource));

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public <R extends BeamResource<B>> void update(Change<?> change, Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(
                cloud,
                filter,
                currentResources,
                pendingResources);

        diff.diff();
        change.getDiffs().add(diff);
    }

    public <R extends BeamResource<B>> void updateOne(Change<?> change, R currentResource, R pendingResource) throws Exception {
        if (currentResource != null) {
            if (pendingResource != null) {
                ResourceDiff diff = new ResourceDiff(
                        cloud,
                        filter,
                        Arrays.asList(currentResource),
                        Arrays.asList(pendingResource));

                diff.diff();
                change.getDiffs().add(diff);

            } else {
                deleteOne(change, currentResource);
            }

        } else if (pendingResource != null) {
            createOne(change, pendingResource);
        }
    }

    public void delete(Change<?> change, Collection<? extends BeamResource<B>> currentResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(
                cloud,
                filter,
                currentResources,
                null);

        diff.diff();
        change.getDiffs().add(diff);
    }

    public void deleteOne(Change<?> change, BeamResource<B> currentResource) throws Exception {
        if (currentResource != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    Arrays.asList(currentResource),
                    null);

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    private boolean isNullable(Method reader) {
        ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.nullable();
        }

        return false;
    }
}
