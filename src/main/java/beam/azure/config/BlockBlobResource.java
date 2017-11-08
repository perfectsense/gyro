package beam.azure.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BlockBlobResource extends AzureResource<CloudBlockBlob> {

    private static final String OBJECT_CONTENT_URL_KEY = "beam_object_content_url";

    private BeamReference container;
    private String key;
    private String objectContentUrl;

    public BeamReference getContainer() {
        return newParentReference(BlobContainerResource.class, container);
    }

    public void setContainer(BeamReference container) {
        this.container = container;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ResourceDiffProperty(updatable = true)
    public String getObjectContentUrl() {
        return objectContentUrl;
    }

    public void setObjectContentUrl(String objectContentUrl) {
        this.objectContentUrl = objectContentUrl;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getContainer(), getKey());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, CloudBlockBlob blockBlob) {
        try {
            setContainer(newReference(BlobContainerResource.class, blockBlob.getContainer().getName()));
            String path = blockBlob.getUri().getPath();
            path = path.substring(path.indexOf("/", 1) + 1);

            setKey(path);
            setObjectContentUrl(blockBlob.getMetadata().get(OBJECT_CONTENT_URL_KEY));

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to load azure block blob: " + getKey()));
        }
    }

    @Override
    public void create(AzureCloud cloud) {
        try {
            String blobUri = "https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/" + getContainer().awsId() + "/" + getKey();
            CloudBlobClient client = createClient(cloud);

            CloudBlockBlob cloudBlockBlob = new CloudBlockBlob(new URI(blobUri), client.getCredentials());
            String contentUrl = getObjectContentUrl();
            cloudBlockBlob.startCopy(new URI(contentUrl));
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put(OBJECT_CONTENT_URL_KEY, contentUrl);
            cloudBlockBlob.setMetadata(metadata);
            cloudBlockBlob.uploadMetadata();

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException(String.format("Unable to create azure block blob: " + getKey()));
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, CloudBlockBlob> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AzureCloud cloud) {
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
    public String toDisplayString() {
        return "block blob " + getKey();
    }
}
