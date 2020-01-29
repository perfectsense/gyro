package gyro.core.vault;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.google.common.base.CaseFormat;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("vault")
public class VaultDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        String type = getArgument(scope, node, String.class, 0);
        String name = getArgument(scope, node, String.class, 1);
        Scope bodyScope = evaluateBody(scope, node);

        VaultSettings settings = scope.getSettings(VaultSettings.class);
        Class<? extends Vault> vaultClass = settings.getVaultClasses().get(type);

        if (vaultClass == null) {
            throw new GyroException("Unable to find a vault implementation named '" + type + "'.");
        }

        Vault vault = Reflections.newInstance(vaultClass);
        vault.setName(name);

        for (PropertyDescriptor property : Reflections.getBeanInfo(vaultClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();

            if (setter != null && !"setName".equals(setter.getName())) {
                Object value = scope.convertValue(
                    setter.getGenericParameterTypes()[0],
                    bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName())));

                if (value != null) {
                    Reflections.invoke(setter, vault, value);
                }
            }
        }

        if (settings.getVaultsByName().containsKey(name)) {
            throw new GyroException("A vault with the name '" + name + "' was previously defined.");
        }

        settings.getVaultsByName().put(name, vault);
    }

}
