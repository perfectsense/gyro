package gyro.azure.storage;

import gyro.azure.AzureResource;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a storage account
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     azure::storage-account file-storage-account-example
 *         resource-group-name: $(azure::resource-group file-resource-group | resource-group-name)
 *         storage-account-name: "storageaccount"
 *
 *         tags: {
 *             Name: "storageaccount"
 *         }
 *     end
 */
@ResourceName("storage-account")
public class StorageAccountResource extends AzureResource {

    private Map<String, String> keys;
    private String resourceGroupName;
    private String storageAccountId;
    private String storageAccountName;
    private Map<String, String> tags;

    @ResourceOutput
    public Map<String, String> getKeys() {
        if (keys == null) {
            keys = new HashMap<>();
        }

        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    @ResourceOutput
    public String getStorageAccountId() {
        return storageAccountId;
    }

    public void setStorageAccountId(String storageAccountId) {
        this.storageAccountId = storageAccountId;
    }

    /**
     * The name of the storage account. (Required)
     */
    public String getStorageAccountName() {
        return storageAccountName;
    }

    public void setStorageAccountName(String storageAccountName) {
        this.storageAccountName = storageAccountName;
    }

    public String storageConnection() {
        return "DefaultEndpointsProtocol=https;"
                + "AccountName=" + getStorageAccountName() + ";"
                + "AccountKey=" + getKeys().get("key1");
    }

    /**
     * The tags for the storage account. (Optional)
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean refresh() {
        Azure client = createClient();

        StorageAccount storageAccount = client.storageAccounts().getById(getStorageAccountId());

        setResourceGroupName(storageAccount.resourceGroupName());
        setStorageAccountId(storageAccount.id());
        setStorageAccountName(storageAccount.name());

        getKeys().clear();
        storageAccount.getKeys().stream().forEach(e -> getKeys().put(e.keyName(), e.value()));

        getTags().clear();
        storageAccount.tags().entrySet().stream().forEach(e -> getTags().put(e.getKey(), e.getValue()));

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        StorageAccount storageAccount = client.storageAccounts()
                .define(getStorageAccountName())
                .withRegion(Region.fromName(getRegion()))
                .withNewResourceGroup(getResourceGroupName())
                .withTags(getTags())
                .create();

        setStorageAccountId(storageAccount.id());

        List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();
        for (StorageAccountKey key : storageAccountKeys) {
            getKeys().put(key.keyName(), key.value());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        StorageAccount storageAccount = client.storageAccounts().getById(getStorageAccountId());
        storageAccount.update().withTags(getTags()).apply();
    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.storageAccounts().deleteById(getStorageAccountId());
    }

    @Override
    public String toDisplayString() {
        return "storage account " + getStorageAccountName();
    }
}
