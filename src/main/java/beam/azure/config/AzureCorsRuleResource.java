package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.CloudBlobClient;

import java.util.*;

public class AzureCorsRuleResource extends AzureResource<CorsRule> {
    private List<String> allowedOrigins;
    private List<String> allowedHeaders;
    private List<String> allowedMethods;
    private int maxAgeSeconds;

    @ResourceDiffProperty(updatable = true)
    public List<String> getAllowedOrigins() {
        if (allowedOrigins == null) {
            allowedOrigins = new ArrayList<>();
        }

        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getAllowedHeaders() {
        if (allowedHeaders == null) {
            allowedHeaders = new ArrayList<>();
        }

        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getAllowedMethods() {
        if (allowedMethods == null) {
            allowedMethods = new ArrayList<>();
        }

        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @ResourceDiffProperty(updatable = true)
    public int getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    @Override
    public String awsId() {
        return String.join("-", getAllowedMethods());
    }

    @Override
    public List<?> diffIds() {
        return getAllowedMethods();
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, CorsRule corsRule) {
        for (CorsHttpMethods methods : corsRule.getAllowedMethods()) {
            getAllowedMethods().add(methods.name());
        }

        setAllowedHeaders(corsRule.getAllowedHeaders());
        setAllowedOrigins(corsRule.getAllowedOrigins());
        setMaxAgeSeconds(corsRule.getMaxAgeInSeconds());
    }

    @Override
    public void create(AzureCloud cloud) {
        CloudBlobClient client = createClient(cloud);

        try {
            ServiceProperties blobServiceProperties = client.downloadServiceProperties();
            CorsProperties cors = blobServiceProperties.getCors();
            CorsRule corsRule = new CorsRule();

            Set<CorsHttpMethods> corsMethods = new HashSet<>();
            for (String method : getAllowedMethods()) {
                corsMethods.add(CorsHttpMethods.valueOf(method));
            }

            EnumSet<CorsHttpMethods> allowedMethods = EnumSet.copyOf(corsMethods);
            List<String> exposedHeaders = new ArrayList<>();
            corsRule.setAllowedHeaders(getAllowedHeaders());
            corsRule.setAllowedMethods(allowedMethods);
            corsRule.setAllowedOrigins(getAllowedOrigins());
            corsRule.setExposedHeaders(exposedHeaders);
            corsRule.setMaxAgeInSeconds(getMaxAgeSeconds());
            cors.getCorsRules().add(corsRule);
            blobServiceProperties.setCors(cors);
            client.uploadServiceProperties(blobServiceProperties);

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to create or update azure cors rule: " + getAllowedMethods()));
        }
    }

    public CloudBlobClient createClient(AzureCloud cloud) {
        String storageConnectionString = "DefaultEndpointsProtocol=https;"
                + "AccountName=" + cloud.getCredentials().getStorageName() + ";"
                + "AccountKey=" + cloud.getCredentials().getStorageKey() + ";"
                + "BlobEndpoint=https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/;"
                + "TableEndpoint=https://" + cloud.getCredentials().getStorageName() + ".table.core.windows.net/;"
                + "QueueEndpoint=https://" + cloud.getCredentials().getStorageName() + ".queue.core.windows.net/;"
                + "FileEndpoint=https://" + cloud.getCredentials().getStorageName() + ".file.core.windows.net/";

        try {
            CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
            return account.createCloudBlobClient();

        } catch (Exception error) {
            throw new BeamException("Fail to create azure storage client!");
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, CorsRule> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        CloudBlobClient client = createClient(cloud);

        try {
            ServiceProperties blobServiceProperties = client.downloadServiceProperties();
            CorsProperties cors = blobServiceProperties.getCors();
            CorsProperties pendingCors = new CorsProperties();

            Set<String> pendingMethods = new HashSet<>();
            pendingMethods.addAll(getAllowedMethods());
            for (CorsRule rule : cors.getCorsRules()) {
                Set<String> currentMethods = new HashSet<>();
                for (CorsHttpMethods method : rule.getAllowedMethods()) {
                    currentMethods.add(method.name());
                }

                if (!pendingMethods.equals(currentMethods)) {
                    pendingCors.getCorsRules().add(rule);
                }
            }

            blobServiceProperties.setCors(pendingCors);
            client.uploadServiceProperties(blobServiceProperties);

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to delete azure cors rule: " + getAllowedMethods()));
        }
    }

    @Override
    public String toDisplayString() {
        return String.format("cors rule %s from %s, header %s", getAllowedMethods(), getAllowedOrigins(), getAllowedHeaders());
    }

}
