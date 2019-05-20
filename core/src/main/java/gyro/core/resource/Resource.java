package gyro.core.resource;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gyro.core.GyroException;
import gyro.core.NamespaceUtils;
import gyro.core.auth.Credentials;
import gyro.core.auth.CredentialsSettings;

public abstract class Resource extends Diffable {

    public abstract boolean refresh();

    public abstract void create();

    public void testCreate() {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            if (field.getTestValue() != null) {
                String value = "test-" + field.getTestValue();

                if (field.isTestValueRandomSuffix())  {
                    value += "-";
                    value += UUID.randomUUID().toString().replaceAll("-", "").substring(16);
                }

                field.setValue(this, value);
            }
        }
    }

    public abstract void update(Resource current, Set<String> changedFieldNames);

    public abstract void delete();

    public Credentials<?> credentials() {
        String name = NamespaceUtils.getNamespacePrefix(getClass()) + "default";

        Credentials<?> credentials = scope.getRootScope()
            .getSettings(CredentialsSettings.class)
            .getCredentialsByName()
            .get(name);

        if (credentials == null) {
            throw new GyroException(String.format(
                "Can't find [%s] credentials!",
                name));
        }

        return credentials;
    }

    public Object get(String key) {
        return Optional.ofNullable(DiffableType.getInstance(getClass()).getField(key))
                .map(f -> f.getValue(this))
                .orElse(null);
    }

}
