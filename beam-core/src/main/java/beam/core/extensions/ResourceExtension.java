package beam.core.extensions;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamReference;

import java.util.ArrayList;
import java.util.List;

public class ResourceExtension extends MethodExtension {

    private String name;
    private Class resourceClass;

    public ResourceExtension(String name, Class resourceClass) {
        this.name = name;
        this.resourceClass = resourceClass;
    }

    @Override
    public String getName() {
        if (name == null) {
            return "resource";
        }

        return name;
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {
        try {
            BeamResource resource = (BeamResource) resourceClass.newInstance();
            BeamCredentials credentials = (BeamCredentials) resource.getResourceCredentialsClass().newInstance();

            BeamConfigKey key = new BeamConfigKey(getName(), arguments.get(0));
            resource.setResourceIdentifier(arguments.get(0));
            resource.setContext(methodContext.getContext());

            if (globalContext.getContext().containsKey(key)) {
                throw new BeamException("Resource with name " + arguments.get(0) + " already exists.");
            }

            globalContext.getContext().put(key, resource);


            // Assign implicit reference to default credentials.
            if (methodContext.get("resourceCredentials") == null) {
                BeamConfigKey credentialsKey = new BeamConfigKey(resource.getResourceCredentialsClass().getSimpleName(), "default");

                List<BeamConfigKey> scopeChain = new ArrayList<>();
                scopeChain.add(credentialsKey);

                BeamReference credentialsReference = new BeamReference();
                credentialsReference.getScopeChain().add(credentialsKey);

                // Add reference to current resource
                BeamConfigKey resourceCredentialsKey = new BeamConfigKey(null, "resourceCredentials");
                methodContext.getContext().put(resourceCredentialsKey, credentialsReference);
            }
        } catch (InstantiationException | IllegalAccessException ie) {
            throw new BeamException("Unable to instantiate " + resourceClass.getSimpleName());
        }
    }

}
