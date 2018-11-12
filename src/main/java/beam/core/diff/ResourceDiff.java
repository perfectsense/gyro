package beam.core.diff;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import beam.core.BeamCloud;
import beam.core.BeamResource;

import com.google.common.base.Throwables;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

public class ResourceDiff<C extends BeamCloud> {

    private final C cloud;
    private final Iterable<? extends BeamResource<C>> currentResources;
    private final Iterable<? extends BeamResource<C>> pendingResources;
    private final List<ResourceChange<?>> changes = new ArrayList<>();

    public ResourceDiff(
            C cloud,
            Iterable<? extends BeamResource<C>> currentResources,
            Iterable<? extends BeamResource<C>> pendingResources) {

        this.cloud = cloud;
        this.currentResources = currentResources;
        this.pendingResources = pendingResources != null ? pendingResources : Collections.<BeamResource<C>>emptySet();
    }

    /**
     * Returns all resources that are currently running in providers.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public Iterable<? extends BeamResource<C>> getCurrentResources() {
        return currentResources;
    }

    /**
     * Returns all resources that should be applied from configs.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public Iterable<? extends BeamResource<C>> getPendingResources() {
        return pendingResources;
    }

    /**
     * Called when a new asset needs to be created based on the given
     * {@code config}.
     *
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newCreate(final BeamResource<C> pendingResource) throws Exception {
        BeamResource<C> currentResource = pendingResource.findCurrent(cloud);

        if (currentResource != null) {
            return newUpdate(currentResource, pendingResource);
        }

        ResourceChange create = new ResourceChange(this, null, pendingResource, cloud) {

            @Override
            protected BeamResource<C> change() {
                pendingResource.resolveDependencies();
                pendingResource.create(cloud);
                cloud.saveState(pendingResource);
                return pendingResource;
            }

            @Override
            public String toString() {
                return String.format("Create %s", pendingResource.toDisplayString());
            }
        };

        pendingResource.setChange(create);
        return create;
    }

    /**
     * Called when the given {@code asset} needs to be updated based on the
     * given {@code config}.
     *
     * @param currentResource Can't be {@code null}.
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newUpdate(final BeamResource<C> currentResource, final BeamResource<C> pendingResource) throws Exception {

        // Fill the empty properties in pending with the values from current.
        try {
            Class klass = pendingResource != null ? pendingResource.getClass() : null;
            if (klass == null && currentResource != null) {
                klass = currentResource.getClass();
            }

            if (klass != null) {
                for (PropertyDescriptor p : Introspector.getBeanInfo(klass).getPropertyDescriptors()) {
                    Method reader = p.getReadMethod();

                    if (reader != null) {
                        Method writer = p.getWriteMethod();

                        Object pendingValue = reader.invoke(pendingResource);
                        if (writer != null && !isNullable(reader) &&
                                (pendingValue == null ||
                                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullArrayList) ||
                                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullSet))) {
                            writer.invoke(pendingResource, reader.invoke(currentResource));
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

        ResourceChange update = new ResourceChange(this, currentResource, pendingResource, cloud);

        currentResource.setChange(update);
        pendingResource.setChange(update);
        return update;
    }

    /**
     * Called when the given {@code asset} needs to be deleted.
     *
     * @param currentResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newDelete(final BeamResource<C> currentResource) throws Exception {
        ResourceChange delete = new ResourceChange(this, currentResource, null, cloud) {

            @Override
            protected BeamResource<C> change() {
                currentResource.delete(cloud);
                cloud.deleteState(currentResource);
                return null;
            }

            @Override
            public String toString() {
                return String.format(
                        "Delete %s",
                        currentResource.toDisplayString());
            }
        };

        currentResource.setChange(delete);

        return delete;
    }

    public void create(ResourceChange<?> change, Collection<? extends BeamResource<C>> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(
                cloud,
                null,
                pendingResources);

        diff.diff();
        change.getDiffs().add(diff);
    }

    public <R extends BeamResource<C>> void update(ResourceChange<?> change, Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(
                cloud,
                currentResources,
                pendingResources);

        diff.diff();
        change.getDiffs().add(diff);
    }

    public void delete(ResourceChange<?> change, Collection<? extends BeamResource<C>> currentResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(
                cloud,
                currentResources,
                null);

        diff.diff();
        change.getDiffs().add(diff);
    }

    private boolean isNullable(Method reader) {
        ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.nullable();
        }

        return false;
    }

    public List<ResourceChange<?>> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        Map<String, BeamResource<C>> currentResourcesByName = new CompactMap<>();
        Iterable<? extends BeamResource<C>> currentResources = getCurrentResources();

        if (currentResources != null) {
            for (BeamResource<C> resource : currentResources) {
                currentResourcesByName.put(resource.getResourceName(), resource);
            }
        }

        Iterable<? extends BeamResource<C>> pendingConfigs = getPendingResources();

        if (pendingConfigs != null) {
            for (BeamResource<C> config : pendingConfigs) {
                String name = config.getResourceName();
                BeamResource<C> asset = currentResourcesByName.remove(name);
                ResourceChange<?> change = asset != null ? newUpdate(asset, config) : newCreate(config);

                if (change != null) {
                    changes.add(change);
                }
            }
        }

        if (currentResources != null) {
            for (BeamResource<C> resource : currentResourcesByName.values()) {
                ResourceChange<?> change = newDelete(resource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    public boolean hasChanges() {
        List<ResourceChange<?>> changes = getChanges();

        for (ResourceChange<?> change : changes) {
            if (change.getType() != ChangeType.KEEP) {
                return true;
            }
        }

        for (ResourceChange<?> change : changes) {
            for (ResourceDiff<?> diff : change.getDiffs()) {
                if (diff.hasChanges()) {
                    return true;
                }
            }
        }

        return false;
    }
}
