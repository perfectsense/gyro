package beam.azure;

import java.util.HashMap;
import java.util.Map;

public class AzureCredentials {
    private String subscription;
    private String clientId;
    private String clientKey;
    private String tenant;
    private String storageName;
    private String storageKey;

    public AzureCredentials(String subscription, String clientId, String clientKey, String tenant, String storageName, String storageKey) {
        this.subscription = subscription;
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.tenant = tenant;
        this.storageName = storageName;
        this.storageKey = storageKey;
    }

    public String getSubscription() {
        return subscription;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getTenant() {
        return tenant;
    }

    public String getStorageName() {
        return storageName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Map<String, String> toMap() {
        Map<String, String> credsMap = new HashMap<>();
        credsMap.put("subscription", subscription);
        credsMap.put("clientId", clientId);
        credsMap.put("clientKey", clientKey);
        credsMap.put("tenant", tenant);
        credsMap.put("storageName", storageName);
        credsMap.put("storageKey", storageKey);
        return credsMap;
    }
}
