package beam.azure.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import com.microsoft.azure.management.compute.models.DataDisk;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudPageBlob;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DiskResource extends AzureResource<DataDisk> {

    private BeamReference virtualMachine;
    private String type;
    private Integer size;
    private String name;
    private Boolean deleteOnTerminate;
    private Integer lun;

    public BeamReference getVirtualMachine() {
        return newParentReference(DiskResource.class, virtualMachine);
    }

    public void setVirtualMachine(BeamReference virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getDeleteOnTerminate() {
        if (deleteOnTerminate == null) {
            deleteOnTerminate = true;
        }

        return deleteOnTerminate;
    }

    public void setDeleteOnTerminate(Boolean deleteOnTerminate) {
        this.deleteOnTerminate = deleteOnTerminate;
    }

    public Integer getLun() {
        return lun;
    }

    public void setLun(Integer lun) {
        this.lun = lun;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, DataDisk dataDisk) {
        try {
            setName(dataDisk.getName());
            setSize(dataDisk.getDiskSizeGB());
            setLun(dataDisk.getLun());

        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to load azure data disk: " + getName()));
        }
    }

    @Override
    public void create(AzureCloud cloud) {

    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, DataDisk> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(AzureCloud cloud) {
        if (getDeleteOnTerminate()) {
            CloudBlobClient client = createClient(cloud);

            try {
                CloudBlobContainer container = client.getContainerReference("vhds");
                CloudPageBlob blob = container.getPageBlobReference(getName() + ".vhd");
                blob.deleteIfExists();

            } catch (Exception error) {
                error.printStackTrace();
                throw new BeamException(String.format("Unable to delete azure disk resource: " + getName()));
            }
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
    public String toDisplayString() {
        return "data disk " + getName() + " " + getSize() + "GB";
    }
}
