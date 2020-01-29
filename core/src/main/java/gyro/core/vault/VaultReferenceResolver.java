package gyro.core.vault;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.reference.ReferenceResolver;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;

/**
 * Examples:
 *
 *     $(vault-lookup secret-key)       - Grab key from the 'default' vault
 *     $(vault-lookup secret-key vault) - Grab key from the vault provided by vault
 */
@Type("vault-lookup")
public class VaultReferenceResolver extends ReferenceResolver {

    @Override
    public Object resolve(Scope scope, List<Object> arguments) throws Exception {

        if (arguments.isEmpty()) {
            throw new GyroException("Missing key parameter. Expected $(vault-lookup <key> <vault>).");
        }

        String key = null;
        String vaultName = "default";
        String hash = null;

        if (arguments.size() == 1) {
            key = (String) arguments.get(0);
        } else if (arguments.size() == 2) {
            key = (String) arguments.get(0);
            vaultName = (String) arguments.get(1);
        } else if (arguments.size() == 3) {
            key = (String) arguments.get(0);
            vaultName = (String) arguments.get(1);
            hash = (String) arguments.get(2);
        }

        RootScope rootScope = scope.getRootScope();
        VaultSettings settings = rootScope.getSettings(VaultSettings.class);
        Vault vault = settings.getVaultsByName().get(vaultName);

        if (vault == null) {
            throw new GyroException("Unable to load the vault named '" + vaultName
                + "'. Ensure the vault is configured in .gyro/init.gyro.");
        }

        return new VaultSecret(key, vaultName, vault.get(key), hash);
    }

}
