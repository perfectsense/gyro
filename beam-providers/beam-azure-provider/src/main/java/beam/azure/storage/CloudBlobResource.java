package beam.azure.storage;

import beam.azure.AzureResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Iterator;
import java.util.Set;

import java.io.File;

/**
 * Creates a cloud blob
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::cloud-blob blob-example
 *         blob-directory-path: "/path/to/blob"
 *         container-name: $(azure::cloud-blob-container blob-container-example | container-name)
 *         file-path: "beam-providers/beam-azure-provider/examples/storage/test-blob-doc.txt"
 *         storage-connection: $(azure::storage-account blob-storage-account-example | storage-connection)
 *     end
 */
@ResourceName("cloud-blob")
public class CloudBlobResource extends AzureResource {

    private String blobName;
    private String blobDirectoryPath;
    private String containerName;
    private String filePath;
    private String storageConnection;

    public String getBlobName() {
        return Paths.get(getBlobDirectoryPath()).getFileName().toString();
    }

    public void setBlobName(String blockBlobName) {
        this.blobName = blobName;
    }

    /**
     * The directory path of the blob. (Required)
     */
    public String getBlobDirectoryPath() {
        return blobDirectoryPath;
    }

    public void setBlobDirectoryPath(String blobDirectoryPath) {
        this.blobDirectoryPath = blobDirectoryPath;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    /**
     * The path of the file to upload. (Required)
     */
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStorageConnection() {
        return storageConnection;
    }

    public void setStorageConnection(String storageConnection) {
        this.storageConnection = storageConnection;
    }

    @Override
    public boolean refresh() {
        try {
            CloudBlockBlob blob = cloudBlobBlob();
            if (blob.exists()) {
                setBlobName(blob.getName());
                return true;
            }
            return false;
        } catch (StorageException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    @Override
    public void create() {
        try {
            CloudBlockBlob blob = cloudBlobBlob();
            File file = new File(getFilePath());
            blob.upload(new FileInputStream(file), file.length());
        } catch (StorageException | IOException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {}

    @Override
    public void delete() {
        try {
            CloudBlockBlob blob = cloudBlobBlob();
            blob.delete();
        } catch (StorageException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    @Override
    public String toDisplayString() {
        return "cloud blob " + getBlobName();
    }

    private CloudBlockBlob cloudBlobBlob() {
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(getStorageConnection());
            CloudBlobClient client = account.createCloudBlobClient();
            CloudBlobContainer container = client.getContainerReference(getContainerName());
            CloudBlockBlob blob;
            if (getBlobDirectoryPath() != null) {
                CloudBlobDirectory directory = createDirectories();
                blob = directory.getBlockBlobReference(getBlobName());
                File file = new File(getFilePath());
                blob.upload(new FileInputStream(file), file.length());
            } else {
                blob = container.getBlockBlobReference(getBlobName());
            }
            return blob;
        } catch (StorageException | URISyntaxException | InvalidKeyException | IOException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    private CloudBlobDirectory createDirectories() {
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(getStorageConnection());
            CloudBlobClient client = account.createCloudBlobClient();
            CloudBlobContainer container = client.getContainerReference(getContainerName());
            Path directoryPath = Paths.get(getBlobDirectoryPath()).getParent();
            Iterator<Path> iter = directoryPath.iterator();
            CloudBlobDirectory directory = container.getDirectoryReference(iter.next().toString());
            while (iter.hasNext()) {
                directory = directory.getDirectoryReference(iter.next().toString());
            }
            return directory;
        } catch (StorageException | URISyntaxException | InvalidKeyException ex) {
            throw new BeamException(ex.getMessage());
        }
    }
}
