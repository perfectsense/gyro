package gyro.core.vault;

import java.util.HashMap;
import java.util.Map;

import gyro.core.scope.Settings;

public class VaultSettings extends Settings {

    private Map<String, Class<? extends Vault>> vaultClasses;
    private Map<String, Vault> vaultsByName;

    /**
     * A map of vault implementations. The key is the name of the vault
     * provided by the vault implementation's @Type annotation.
     */
    public Map<String, Class<? extends Vault>> getVaultClasses() {
        if (vaultClasses == null) {
            vaultClasses = new HashMap<>();
            vaultClasses.put("local", LocalVault.class);
        }

        return vaultClasses;
    }

    public void setVaultClasses(Map<String, Class<? extends Vault>> vaultClasses) {
        this.vaultClasses = vaultClasses;
    }

    /**
     * A map of vaults loaded using the @vault command. The key is the name of the vault
     * provided to the @vault command, or 'default' if one wasn't provided.
     */
    public Map<String, Vault> getVaultsByName() {
        if (vaultsByName == null) {
            vaultsByName = new HashMap<>();
        }

        return vaultsByName;
    }

    public void setVaultsByName(Map<String, Vault> vaultsByName) {
        this.vaultsByName = vaultsByName;
    }

}
