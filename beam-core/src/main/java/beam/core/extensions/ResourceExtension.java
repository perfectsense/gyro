package beam.core.extensions;

import beam.core.BeamException;
import beam.core.BeamResource;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;

import java.util.List;

public class ResourceExtension extends MethodExtension {

    private Class resourceClass;

    public ResourceExtension(Class resourceClass) {
        this.resourceClass = resourceClass;
    }

    @Override
    public String getName() {
        return "resource";
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {
        try {
            BeamResource resource = (BeamResource) resourceClass.newInstance();
            BeamConfigKey key = new BeamConfigKey(resourceClass.getSimpleName(), arguments.get(0));
            resource.setContext(methodContext.getContext());
            globalContext.getContext().put(key, resource);
        } catch (InstantiationException | IllegalAccessException ie) {
            throw new BeamException("Unable to instantiate " + resourceClass.getSimpleName());
        }
    }

}
