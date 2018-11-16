package beam.core.extensions;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;

import java.util.List;

public class CredentialsExtension extends MethodExtension {

    private Class credentialsClass;

    public CredentialsExtension(Class credentialsClass) {
        this.credentialsClass = credentialsClass;
    }

    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {
        try {
            BeamCredentials credentials = (BeamCredentials) credentialsClass.newInstance();
            BeamConfigKey key = new BeamConfigKey(credentialsClass.getSimpleName(), arguments.get(0));
            credentials.setContext(methodContext.getContext());
            globalContext.getContext().put(key, credentials);
        } catch (InstantiationException | IllegalAccessException ie) {
            throw new BeamException("Unable to instantiate " + credentialsClass.getSimpleName());
        }
    }

}
