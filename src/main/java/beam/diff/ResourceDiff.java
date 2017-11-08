package beam.diff;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import beam.BeamCloud;
import beam.BeamResource;
import beam.BeamResourceFilter;

import com.google.common.base.Throwables;
import com.psddev.dari.util.ObjectUtils;

public class ResourceDiff<B extends BeamCloud, A> extends Diff<ResourceDiffKey, BeamResource<B, A>, BeamResource<B, A>> {

    private final B cloud;
    private final BeamResourceFilter filter;
    private final Class<?> configClass;
    private final Iterable<? extends BeamResource<B, A>> currentResources;
    private final Iterable<? extends BeamResource<B, A>> pendingResources;

    public ResourceDiff(
            B cloud,
            BeamResourceFilter filter,
            Class<?> configClass,
            Iterable<? extends BeamResource<B, A>> currentResources,
            Iterable<? extends BeamResource<B, A>> pendingResources) {

        this.cloud = cloud;
        this.filter = filter;
        this.configClass = configClass;

        List<BeamResource<B, A>> crs = new ArrayList<>();

        if (currentResources != null) {
            for (BeamResource<B, A> cr : currentResources) {
                if (filter == null || filter.isInclude(cr)) {
                    crs.add(cr);
                }
            }
        }

        this.currentResources = crs;
        this.pendingResources = pendingResources != null ? pendingResources : Collections.<BeamResource<B, A>>emptySet();
    }

    @Override
    public Iterable<? extends BeamResource<B, A>> getCurrentAssets() {
        return currentResources;
    }

    @Override
    public ResourceDiffKey getIdFromAsset(BeamResource<B, A> asset) {
        return new ResourceDiffKey(asset.diffIds());
    }

    @Override
    public Iterable<? extends BeamResource<B, A>> getPendingConfigs() {
        return pendingResources;
    }

    @Override
    public ResourceDiffKey getIdFromConfig(BeamResource<B, A> config) {
        return new ResourceDiffKey(config.diffIds());
    }

    @Override
    public ResourceChange newCreate(final BeamResource<B, A> pending) throws Exception {
        BeamResource<B, A> current = pending.findCurrent(cloud, filter);

        if (current != null) {
            return newUpdate(current, pending);
        }

        ResourceChange create = new ResourceChange(this, ChangeType.CREATE, null, pending) {

            @Override
            protected BeamResource<B, ?> change() {
                pending.create(cloud);
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
    public ResourceChange newUpdate(final BeamResource<B, A> current, final BeamResource<B, A> pending) throws Exception {

        // Fill the empty properties in pending with the values from current.
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(configClass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    Method writer = p.getWriteMethod();

                    Object pendingValue = reader.invoke(pending);
                    if (writer != null && (pendingValue == null ||
                            (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullArrayList) ||
                            (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullSet))) {
                        writer.invoke(pending, reader.invoke(current));
                    }
                }
            }

        } catch (IllegalAccessException |
                IntrospectionException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        ResourceUpdate update = new ResourceUpdate(this, current, pending, cloud, configClass);

        current.setChange(update);
        pending.setChange(update);
        pending.diffOnUpdate(update, current);
        return update;
    }

    @Override
    public ResourceChange newDelete(final BeamResource<B, A> current) throws Exception {
        if (!current.isDeletable()) {
            return null;
        }

        ResourceChange delete = new ResourceChange(this, ChangeType.DELETE, current, null) {

            @Override
            protected BeamResource<B, ?> change() {
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

    @Override
    public String toString() {
        return configClass.getSimpleName() + ":";
    }

    private Class<?> findFirstClass(Collection<?> objects) {
        if (objects != null) {
            for (Object o : objects) {
                if (o != null) {
                    return o.getClass();
                }
            }
        }

        return null;
    }

    public void create(Change<?> change, Collection<? extends BeamResource<B, ?>> pendingResources) throws Exception {
        Class<?> resourceClass = findFirstClass(pendingResources);

        if (resourceClass != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    resourceClass,
                    null,
                    pendingResources);

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public void createOne(Change<?> change, BeamResource<B, ?> pendingResource) throws Exception {
        if (pendingResource != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    pendingResource.getClass(),
                    null,
                    Arrays.asList(pendingResource));

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public <R extends BeamResource<B, ?>> void update(Change<?> change, Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        Class<?> resourceClass = findFirstClass(currentResources);

        if (resourceClass == null) {
            resourceClass = findFirstClass(pendingResources);
        }

        if (resourceClass != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    resourceClass,
                    currentResources,
                    pendingResources);

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public <R extends BeamResource<B, ?>> void updateOne(Change<?> change, R currentResource, R pendingResource) throws Exception {
        if (currentResource != null) {
            if (pendingResource != null) {
                ResourceDiff diff = new ResourceDiff(
                        cloud,
                        filter,
                        currentResource.getClass(),
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

    public void delete(Change<?> change, Collection<? extends BeamResource<B, ?>> currentResources) throws Exception {
        Class<?> resourceClass = findFirstClass(currentResources);

        if (resourceClass != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    resourceClass,
                    currentResources,
                    null);

            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public void deleteOne(Change<?> change, BeamResource<B, ?> currentResource) throws Exception {
        if (currentResource != null) {
            ResourceDiff diff = new ResourceDiff(
                    cloud,
                    filter,
                    currentResource.getClass(),
                    Arrays.asList(currentResource),
                    null);

            diff.diff();
            change.getDiffs().add(diff);
        }
    }
}
