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
}
