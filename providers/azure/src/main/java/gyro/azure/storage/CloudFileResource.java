package gyro.azure.storage;

import gyro.azure.AzureResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileDirectory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Set;
import java.io.File;

/**
 * Creates a cloud file
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     azure::cloud-file cloud-file-example
 *         cloud-file-directory-path: $(azure::cloud-file-directory cloud-file-directory | cloud-file-directory-path)
 *         cloud-file-share-name: $(azure::cloud-file-share cloud-file-share-example | cloud-file-share-name)
 *         file-path: "gyro-providers/gyro-azure-provider/examples/storage/test-cloud-file.txt"
 *         storage-connection: $(azure::storage-account blob-storage-account-example | storage-connection)
 *     end
 */
@ResourceName("cloud-file")
public class CloudFileResource extends AzureResource {

    private String cloudFileDirectoryPath;
    private String cloudFileShareName;
    private String filePath;
    private String fileName;
    private String storageConnection;

    /**
     * The directory path for the file. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getCloudFileDirectoryPath() {
        return cloudFileDirectoryPath;
    }

    public void setCloudFileDirectoryPath(String cloudFileDirectoryPath) {
        this.cloudFileDirectoryPath = cloudFileDirectoryPath;
    }

    /**
     * The name of the cloud file share for the file. (Required)
     */
    public String getCloudFileShareName() {
        return cloudFileShareName;
    }

    public void setCloudFileShareName(String cloudFileShareName) {
        this.cloudFileShareName = cloudFileShareName;
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

    public String getFileName() {
        File f = new File(getFilePath());
        return f.getName();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
            CloudFile file = cloudFile();
            if (file.exists()) {
                setFileName(file.getName());
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
            CloudFile file = cloudFile();
            file.uploadFromFile(getFilePath());
        } catch (StorageException | URISyntaxException | IOException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {}

    @Override
    public void delete() {
        try {
            CloudFile file = cloudFile();
            file.delete();
        } catch (StorageException | URISyntaxException ex) {
            throw new BeamException(ex.getMessage());
        }
    }

    @Override
    public String toDisplayString() {
        return "cloud file " + getFileName();
    }

    private CloudFile cloudFile() {
        try {
            String name = Paths.get(getFilePath()).getFileName().toString();
            CloudFileDirectoryResource rootDirectory =
                    new CloudFileDirectoryResource(getCloudFileDirectoryPath(), getCloudFileShareName(), getStorageConnection());
            CloudFileDirectory root = rootDirectory.cloudFileDirectory();
            return root.getFileReference(name);
        } catch (StorageException | URISyntaxException ex) {
            throw new BeamException(ex.getMessage());
        }
    }
}
