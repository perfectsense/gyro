package beam.azure;

import beam.BeamException;
import beam.BeamStorage;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class AzureStorage extends BeamStorage {

    private AzureCloud cloud;
    private String container;
    private CloudBlobClient client = null;

    public AzureStorage(AzureCloud cloud, String container) {
        this.cloud = cloud;
        this.container = container;

        String storageConnectionString = "DefaultEndpointsProtocol=https;"
                + "AccountName=" + cloud.getCredentials().getStorageName() + ";"
                + "AccountKey=" + cloud.getCredentials().getStorageKey() + ";"
                + "BlobEndpoint=https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/;"
                + "TableEndpoint=https://" + cloud.getCredentials().getStorageName() + ".table.core.windows.net/;"
                + "QueueEndpoint=https://" + cloud.getCredentials().getStorageName() + ".queue.core.windows.net/;"
                + "FileEndpoint=https://" + cloud.getCredentials().getStorageName() + ".file.core.windows.net/";

        try {
            CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
            this.client = account.createCloudBlobClient();
        } catch (Exception error) {
            throw new BeamException("Fail to create azure storage client!");
        }
    }

    @Override
    public InputStream get(String path) throws IOException {
        return get(path, null);
    }

    @Override
    public InputStream get(String path, String region) throws IOException {
        if (region == null) {
            region = "eastus";
        }

        for (CloudBlobContainer container : client.listContainers(this.container)) {
            CloudBlobContainer currentContainer = container;

            try {
                CloudBlockBlob cloudBlockBlob = currentContainer.getBlockBlobReference(path);
                return cloudBlockBlob.openInputStream();

            } catch (Exception error) {
            }
        }

        return null;
    }

    @Override
    public void put(String region, String path, InputStream content, String contentType, long length) throws IOException {
        if (region == null) {
            region = "eastus";
        }

        try {
            if (content != null) {
                CloudBlobContainer cloudBlobContainer;
                CloudBlockBlob cloudBlockBlob;

                if (!doesExist(region)) {
                    String containerUri = "https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/" + container;
                    cloudBlobContainer = new CloudBlobContainer(new URI(containerUri), client.getCredentials());
                    cloudBlobContainer.create();
                } else {
                    cloudBlobContainer = client.getContainerReference(container);
                }

                boolean blobExist = false;
                for (ListBlobItem item : cloudBlobContainer.listBlobs(path)) {
                    blobExist = true;
                }

                if (!blobExist) {
                    String blobUri = "https://" + cloud.getCredentials().getStorageName() + ".blob.core.windows.net/" + container + "/" + path;
                    cloudBlockBlob = new CloudBlockBlob(new URI(blobUri), client.getCredentials());
                } else {
                    cloudBlockBlob = cloudBlobContainer.getBlockBlobReference(path);
                }

                cloudBlockBlob.upload(content, length);


            } else if (doesExist(region)) {
                CloudBlobContainer cloudBlobContainer = client.getContainerReference(container);
                CloudBlockBlob cloudBlockBlob = cloudBlobContainer.getBlockBlobReference(path);
                cloudBlockBlob.delete();
            }

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to save to azure storage %s:%s", container, path), error);
        }
    }

    @Override
    public boolean doesExist(String region) {

        if (region == null) {
            region = "eastus";
        }

        for (CloudBlobContainer container : client.listContainers(this.container)) {
            return true;
        }

        return false;
    }
}
