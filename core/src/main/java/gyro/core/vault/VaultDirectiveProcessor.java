package gyro.core.vault;

import com.google.common.base.CaseFormat;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;

public class VaultDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "vault";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 2);
        String type = (String) arguments.get(0);
        String name = arguments.size() == 1 ? "default" : (String) arguments.get(1);
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
                Reflections.invoke(setter, vault, scope.convertValue(
                    setter.getGenericParameterTypes()[0],
                    bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        if (settings.getVaultsByName().containsKey(name)) {
            throw new GyroException("A vault with the name '" + name + "' was previously defined.");
        }

        settings.getVaultsByName().put(name, vault);
    }


}
