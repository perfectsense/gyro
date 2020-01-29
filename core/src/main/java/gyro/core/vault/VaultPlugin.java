package gyro.core.vault;

import java.util.Optional;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class VaultPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) throws Exception {
        if (Vault.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Vault> vaultClass = (Class<? extends Vault>) aClass;

            String type = Optional.ofNullable(vaultClass.getAnnotation(Type.class))
                .map(Type::value)
                .orElse(null);

            if (type != null) {
                root.getSettings(VaultSettings.class)
                    .getVaultClasses()
                    .put(type, vaultClass);
            } else {
                throw new GyroException("Loading vault plugin failed. Vault implementation is missing @Type annotation.");
            }
        }
    }

}
