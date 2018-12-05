package beam.core.diff;

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
import java.util.Map;

import beam.core.BeamResource;

import beam.lang.BeamContextKey;
import beam.lang.BeamLiteral;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

public class ResourceDiff {

    private final Iterable<? extends BeamResource> currentResources;
    private final Iterable<? extends BeamResource> pendingResources;
    private final List<ResourceChange> changes = new ArrayList<>();

    public ResourceDiff(Iterable<? extends BeamResource> currentResources,
                        Iterable<? extends BeamResource> pendingResources) {
        this.currentResources = currentResources;
        this.pendingResources = pendingResources != null ? pendingResources : Collections.<BeamResource>emptySet();
    }

    /**
     * Returns all resources that are currently running in providers.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public Iterable<? extends BeamResource> getCurrentResources() {
        return currentResources;
    }

    /**
     * Returns all resources that should be applied from configs.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public Iterable<? extends BeamResource> getPendingResources() {
        return pendingResources;
    }

    /**
     * Called when a new asset needs to be created based on the given
     * {@code config}.
     *
     * @param pendingResource Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public ResourceChange newCreate(final BeamResource pendingResource) throws Exception {
        BeamResource currentResource = pendingResource.findCurrent();

        if (currentResource != null) {
            return newUpdate(currentResource, pendingResource);
        }

        ResourceChange create = new ResourceChange(this, null, pendingResource) {

            @Override
            protected BeamResource change() {
                pendingResource.resolve(pendingResource.getRoot());
                pendingResource.create();
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
    public ResourceChange newUpdate(final BeamResource currentResource, final BeamResource pendingResource) throws Exception {

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
                            if (reader.invoke(currentResource) != null) {
                                String keyId = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, p.getName());
                                pendingResource.addReferable(new BeamContextKey(null, keyId), new BeamLiteral((String) reader.invoke(currentResource)));
                            }
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

        ResourceChange update = new ResourceChange(this, currentResource, pendingResource);
        update.tryToKeep();

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
    public ResourceChange newDelete(final BeamResource currentResource) throws Exception {
        ResourceChange delete = new ResourceChange(this, currentResource, null) {

            @Override
            protected BeamResource change() {
                currentResource.delete();
                return currentResource;
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

    public void create(ResourceChange change, Collection<? extends BeamResource> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(null, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void createOne(ResourceChange change, BeamResource pendingResource) throws Exception {
        if (pendingResource != null) {
            ResourceDiff diff = new ResourceDiff(null, Arrays.asList(pendingResource));
            diff.diff();
            change.getDiffs().add(diff);
        }
    }

    public <R extends BeamResource> void update(ResourceChange change, Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentResources, pendingResources);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public <R extends BeamResource> void updateOne(ResourceChange change, R currentResource, R pendingResource) throws Exception {
        if (currentResource != null) {
            if (pendingResource != null) {
                ResourceDiff diff = new ResourceDiff(Arrays.asList(currentResource), Arrays.asList(pendingResource));
                diff.diff();
                change.getDiffs().add(diff);

            } else {
                deleteOne(change, currentResource);
            }

        } else if (pendingResource != null) {
            createOne(change, pendingResource);
        }
    }

    public void delete(ResourceChange change, Collection<? extends BeamResource> currentResources) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentResources, null);
        diff.diff();
        change.getDiffs().add(diff);
    }

    public void deleteOne(ResourceChange change, BeamResource currentResource) throws Exception {
        if (currentResource != null) {
            ResourceDiff diff = new ResourceDiff(Arrays.asList(currentResource), null);
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

    public List<ResourceChange> getChanges() {
        return changes;
    }

    public void diff() throws Exception {
        Map<String, BeamResource> currentResourcesByName = new CompactMap<>();
        Iterable<? extends BeamResource> currentResources = getCurrentResources();

        if (currentResources != null) {
            for (BeamResource resource : currentResources) {
                currentResourcesByName.put(resource.getResourceIdentifier(), resource);
            }
        }

        Iterable<? extends BeamResource> pendingConfigs = getPendingResources();

        if (pendingConfigs != null) {
            for (BeamResource config : pendingConfigs) {
                String name = config.getResourceIdentifier();
                BeamResource asset = currentResourcesByName.remove(name);
                ResourceChange change = asset != null ? newUpdate(asset, config) : newCreate(config);

                if (change != null) {
                    changes.add(change);
                }
            }
        }

        if (currentResources != null) {
            for (BeamResource resource : currentResourcesByName.values()) {
                ResourceChange change = newDelete(resource);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    public boolean hasChanges() {
        List<ResourceChange> changes = getChanges();

        for (ResourceChange change : changes) {
            if (change.getType() != ChangeType.KEEP) {
                return true;
            }
        }

        for (ResourceChange change : changes) {
            for (ResourceDiff diff : change.getDiffs()) {
                if (diff.hasChanges()) {
                    return true;
                }
            }
        }

        return false;
    }
}
