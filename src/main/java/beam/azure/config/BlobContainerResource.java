package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.google.common.collect.Lists;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlobContainerResource extends AzureResource<CloudBlobContainer> {

    private String name;
    private String accessPolicy;
    private List<BlockBlobResource> blockBlobs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAccessPolicy() {
        if (accessPolicy == null) {
            accessPolicy = "private";
        }

        return accessPolicy;
    }

    public void setAccessPolicy(String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public List<BlockBlobResource> getBlockBlobs() {
        if (blockBlobs == null) {
            blockBlobs = new ArrayList<>();
        }

        return blockBlobs;
    }

    public void setBlockBlobs(List<BlockBlobResource> blockBlobs) {
        this.blockBlobs = blockBlobs;
    }

    @Override
    public String awsId() {
        return getName();
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, CloudBlobContainer container) {
        setName(container.getName());

        try {
            BlobContainerPermissions permissions = container.downloadPermissions();

            if (permissions.getPublicAccess().compareTo(BlobContainerPublicAccessType.OFF) == 0) {
                setAccessPolicy("private");
            } else if (permissions.getPublicAccess().compareTo(BlobContainerPublicAccessType.BLOB) == 0) {
                setAccessPolicy("blob");
            } else if (permissions.getPublicAccess().compareTo(BlobContainerPublicAccessType.CONTAINER) == 0) {
                setAccessPolicy("container");
            }

            for (ListBlobItem item : container.listBlobs("", true)) {
                String path = item.getUri().getPath();
                path = path.substring(path.indexOf("/", 1) + 1);

                if (CloudBlockBlob.class == item.getClass()) {
                    CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(path);
                    cloudBlockBlob.downloadAttributes();

                    BlockBlobResource blobResource = new BlockBlobResource();
                    blobResource.init(cloud, filter, cloudBlockBlob);
                    getBlockBlobs().add(blobResource);
                }
            }

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to load azure blob container: " + getName()));
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getBlockBlobs());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, CloudBlobContainer> current) throws Exception {
        BlobContainerResource currentContainer = (BlobContainerResource) current;
        update.update(currentContainer.getBlockBlobs(), getBlockBlobs());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getBlockBlobs());
    }

    @Override
    public void create(AzureCloud cloud) {

        CloudBlobClient client = createClient(cloud);

        try {
            CloudBlobContainer cloudBlobContainer;
            String containerUri = "https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/" + getName();
            cloudBlobContainer = new CloudBlobContainer(new URI(containerUri), client.getCredentials());

            BlobContainerPermissions permissions = new BlobContainerPermissions();

            if (getAccessPolicy().toLowerCase().equals("private")) {
                permissions.setPublicAccess(BlobContainerPublicAccessType.OFF);
            } else if (getAccessPolicy().toLowerCase().equals("blob")) {
                permissions.setPublicAccess(BlobContainerPublicAccessType.BLOB);
            } else if (getAccessPolicy().toLowerCase().equals("container")) {
                permissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
            }

            cloudBlobContainer.createIfNotExists();
            cloudBlobContainer.uploadPermissions(permissions);

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to create or update azure blob container: " + getName()));
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, CloudBlobContainer> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {

    }

    @Override
    public boolean isDeletable() {
        return false;
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
        return "blob container " + getName();
    }
}
