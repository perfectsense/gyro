package beam.diff;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import beam.BeamCloud;
import beam.BeamResource;

public abstract class ResourceChange<B extends BeamCloud> extends Change<BeamResource<B, ?>> {

    private final ResourceDiff diff;
    private final BeamResource<B, ?> resource;

    public ResourceChange(ResourceDiff diff, ChangeType type, BeamResource<B, ?> current, BeamResource<B, ?> pending) {
        super(type, current);

        this.diff = diff;
        this.resource = pending != null ? pending : current;
    }

    public BeamResource<B, ?> getResource() {
        return resource;
    }

    public void create(Collection<? extends BeamResource<B, ?>> pendingResources) throws Exception {
        diff.create(this, pendingResources);
    }

    public void createOne(BeamResource<B, ?> pendingResource) throws Exception {
        diff.createOne(this, pendingResource);
    }

    public <R extends BeamResource<B, ?>> void update(Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        diff.update(this, currentResources, pendingResources);
    }

    public <R extends BeamResource<B, ?>> void updateOne(R currentResource, R pendingResource) throws Exception {
        diff.updateOne(this, currentResource, pendingResource);
    }

    public void delete(Collection<? extends BeamResource<B, ?>> pendingResources) throws Exception {
        diff.delete(this, pendingResources);
    }

    public void deleteOne(BeamResource<B, ?> pendingResource) throws Exception {
        diff.deleteOne(this, pendingResource);
    }

    @Override
    public Set<Change<?>> dependencies() {
        Set<Change<?>> dependencies = new HashSet<>();

        for (BeamResource<B, ?> r : (getType() == ChangeType.DELETE ?
                resource.dependents() :
                resource.dependencies())) {

            Change<?> c = r.getChange();

            if (c != null) {
                dependencies.add(c);
            }
        }

        return dependencies;
    }
}
