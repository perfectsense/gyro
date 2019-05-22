package gyro.core.auth;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.base.CaseFormat;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;

public class CredentialsDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Scope scope, List<Object> arguments) throws Exception {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@credentials directive can only be used within the init.gyro file!");
        }

        int argumentsSize = arguments.size();

        if (argumentsSize < 2 || argumentsSize > 3) {
            throw new GyroException("@credentials directive only takes 2 or 3 arguments!");
        }

        String type = (String) arguments.get(0);
        String name;
        Map<String, Object> values;

        if (argumentsSize == 2) {
            name = "default";
            values = (Map<String, Object>) arguments.get(1);

        } else {
            name = (String) arguments.get(1);
            values = (Map<String, Object>) arguments.get(2);
        }

        RootScope root = (RootScope) scope;
        CredentialsSettings settings = root.getSettings(CredentialsSettings.class);
        Class<? extends Credentials> credentialsClass = settings.getCredentialsClasses().get(type);
        Credentials credentials = credentialsClass.newInstance();
        credentials.scope = scope;

        for (PropertyDescriptor property : Introspector.getBeanInfo(credentialsClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();

            if (setter != null) {
                setter.invoke(credentials, root.convertValue(
                    setter.getGenericParameterTypes()[0],
                    values.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        credentials.getNamespaces().stream()
            .map(ns -> ns + "::" + name)
            .forEach(n -> settings.getCredentialsByName().put(n, credentials));
    }

}