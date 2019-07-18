package gyro.core.vault;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.command.AbstractCommand;
import gyro.core.scope.RootScope;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Command(name = "vault", description = "Store/retrieve secrets")
public class VaultCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments = new ArrayList<>();

    @Option(name = { "--vault" }, description = "The vault name to manipulate or query.")
    private String vaultName = "default";

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getVaultName() {
        return vaultName;
    }

    public void setVaultName(String vaultName) {
        this.vaultName = vaultName;
    }

    @Override
    protected void doExecute() throws Exception {
        if (getArguments().isEmpty()) {
            throw new GyroException("Expected 'gyro vault get|put key' or 'gyro vault list'");
        }

        RootScope scope = (RootScope) getInit();
        VaultSettings settings = scope.getSettings(VaultSettings.class);
        Vault vault = settings.getVaultsByName().get(getVaultName());

        if (vault == null) {
            throw new GyroException("Unable to load the vault named '" + getVaultName() + "'. Ensure the vault is configured in .gyro/init.gyro.");
        }

        String command = getArguments().get(0);

        if ("get".equalsIgnoreCase(command)) {
            String key = getArguments().size() >= 2 ? getArguments().get(1) : null;

            if (key == null) {
                throw new GyroException("Key argument missing. Expected 'gyro vault get <key>'");
            }

            String secret = vault.get(key);
            if (secret == null) {
                GyroCore.ui().write("");
            } else {
                GyroCore.ui().write(vault.get(key) + "\n");
            }
        } else if ("put".equalsIgnoreCase(command)) {
            String key = getArguments().size() >= 2 ? getArguments().get(1) : null;
            String value = getArguments().size() >= 3 ? getArguments().get(2) : null;

            if (key == null || value == null) {
                throw new GyroException("Key or value argument missing. Expected 'gyro vault put <key> <value>'");
            }

            if (vault.put(key, value)) {
                GyroCore.ui().write("\nKey '%s' was overwritten in the '%s' vault.\n", key, getVaultName());
            } else {
                GyroCore.ui().write("\nKey '%s' was written to the '%s' vault.\n", key, getVaultName());
            }
        } else if ("list".equalsIgnoreCase(command)) {
            String prefix = getArguments().size() == 2 ? getArguments().get(1) : null;

            GyroCore.ui().write("\n");
            Map<String, String> secrets = vault.list(prefix);
            for (String key : secrets.keySet()) {
                GyroCore.ui().write("%s: %s\n", key, secrets.get(key));
            }
        } else {
            throw new GyroException("Unknown command. Valid commands are: get and put");
        }

    }

}
