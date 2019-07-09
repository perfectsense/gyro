package gyro.core.auth;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.base.CaseFormat;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class CredentialsDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        List<Object> arguments = evaluateArguments(scope, node);
        int argumentsSize = arguments.size();

        if (argumentsSize < 1 || argumentsSize > 2) {
            throw new GyroException("@credentials directive only takes 1 or 2 arguments!");
        }

        String type = (String) arguments.get(0);
        String name = argumentsSize == 1 ? "default" : (String) arguments.get(1);
        Scope bodyScope = evaluateBody(scope, node);

        CredentialsSettings settings = scope.getSettings(CredentialsSettings.class);
        Class<? extends Credentials> credentialsClass = settings.getCredentialsClasses().get(type);
        Credentials credentials = credentialsClass.newInstance();
        credentials.scope = scope;

        for (PropertyDescriptor property : Introspector.getBeanInfo(credentialsClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();

            if (setter != null) {
                setter.invoke(credentials, scope.convertValue(
                    setter.getGenericParameterTypes()[0],
                    bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        credentials.getNamespaces().stream()
            .map(ns -> ns + "::" + name)
            .forEach(n -> settings.getCredentialsByName().put(n, credentials));
    }

}