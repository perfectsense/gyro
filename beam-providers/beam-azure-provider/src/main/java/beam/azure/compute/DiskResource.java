package beam.azure.compute;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.core.diff.ResourceOutput;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.DiskSkuTypes;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates a disk.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::disk disk-example
 *          disk-name: "disk-example"
 *          disk-type: "Standard_LRS"
 *          os-type: "LINUX"
 *          size: 10
 *          resource-group-name: $(azure::resource-group resource-group-disk-example | resource-group-name)
 *          tags: {
 *              Name: "disk-example"
 *          }
 *     end
 */
@ResourceName("disk")
public class DiskResource extends AzureResource {
    private String diskName;
    private String diskId;
    private String resourceGroupName;
    private Integer size;
    private String osType;
    private String diskType;
    private String dataLoadSourceType;
    private String dataLoadSource;
    private Map<String, String> tags;

    /**
     * Name of the disk. (Required)
     */
    public String getDiskName() {
        return diskName;
    }

    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }

    @ResourceOutput
    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    /**
     * Name of the resource group under which this would reside. (Required)
     */
    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    /**
     * Size of the disk in Gb. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * Type of OS. Valid options include [ 'LINUX', 'WINDOWS'].
     */
    @ResourceDiffProperty(updatable = true)
    public String getOsType() {
        return osType != null ? osType.toUpperCase() : null;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    /**
     * Type of Disk. Valid options include [ 'STANDARD_LRS', 'PREMIUM_LRS', 'STANDARD_SSD_LRS'].
     */
    @ResourceDiffProperty(updatable = true)
    public String getDiskType() {
        return diskType != null ? diskType.toUpperCase() : null;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }

    /**
     * Type of data source. Defaults to 'disk'. Valid options include [ 'disk', 'vhd', 'snapshot'].
     */
    public String getDataLoadSourceType() {
        if (dataLoadSourceType == null) {
            dataLoadSourceType = "disk";
        }
        return dataLoadSourceType;
    }

    public void setDataLoadSourceType(String dataLoadSourceType) {
        this.dataLoadSourceType = dataLoadSourceType;
    }

    /**
     * The actual data source.
     */
    public String getDataLoadSource() {
        return dataLoadSource;
    }

    public void setDataLoadSource(String dataLoadSource) {
        this.dataLoadSource = dataLoadSource;
    }

    @ResourceDiffProperty(updatable = true)
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

        Disk disk = client.disks().getById(getDiskId());
        setDiskName(disk.name());
        setOsType(disk.osType() != null ? disk.osType().name() : null);
        setSize(disk.sizeInGB());
        setDiskType(disk.sku().accountType().toString());
        setTags(disk.tags());
        setResourceGroupName(disk.resourceGroupName());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        Disk.DefinitionStages.WithDiskSource diskDefWithoutData = client.disks()
            .define(getDiskName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName());

        Disk disk;

        if (!ObjectUtils.isBlank(getDataLoadSource())) {
            Disk.DefinitionStages.WithCreateAndSize diskDefWithData;
            if (getOsType().equals("LINUX")) {
                //Linux
                if (getDataLoadSourceType().equals("vhd")) {
                    diskDefWithData = diskDefWithoutData.withLinuxFromVhd(getDataLoadSource());
                } else if (getDataLoadSourceType().equals("snapshot")) {
                    diskDefWithData = diskDefWithoutData.withLinuxFromSnapshot(getDataLoadSource());
                } else {
                    //disk
                    diskDefWithData = diskDefWithoutData.withLinuxFromDisk(getDataLoadSource());
                }
            } else {
                //Windows
                if (getDataLoadSourceType().equals("vhd")) {
                    diskDefWithData = diskDefWithoutData.withWindowsFromVhd(getDataLoadSource());
                } else if (getDataLoadSourceType().equals("snapshot")) {
                    diskDefWithData = diskDefWithoutData.withWindowsFromSnapshot(getDataLoadSource());
                } else {
                    //disk
                    diskDefWithData = diskDefWithoutData.withWindowsFromDisk(getDataLoadSource());
                }
            }

            disk = diskDefWithData.withSizeInGB(getSize())
                .withTags(getTags())
                .withSku(DiskSkuTypes.fromStorageAccountType(StorageAccountTypes.fromString(getDiskType())))
                .create();

        } else {
            disk = diskDefWithoutData.withData()
                .withSizeInGB(getSize())
                .withTags(getTags())
                .withSku(DiskSkuTypes.fromStorageAccountType(StorageAccountTypes.fromString(getDiskType())))
                .create();

            disk.update().withOSType(OperatingSystemTypes.fromString(getOsType())).apply();
        }

        setDiskId(disk.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        Disk disk = client.disks().getById(getDiskId());

        int changeCount = 0;

        if (changedProperties.contains("os-type")) {
            if (!ObjectUtils.isBlank(getOsType())) {
                disk.update()
                    .withOSType(OperatingSystemTypes.fromString(getOsType()))
                    .apply();
            }

            changeCount++;
        }

        if (changedProperties.size() > changeCount) {
            disk.update()
                .withSizeInGB(getSize())
                .withSku(DiskSkuTypes.fromStorageAccountType(StorageAccountTypes.fromString(getDiskType())))
                .withTags(getTags())
                .apply();
        }
    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.disks().deleteById(getDiskId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("disk");

        if (!ObjectUtils.isBlank(getDiskName())) {
            sb.append(" - ").append(getDiskName());
        }

        if (!ObjectUtils.isBlank(getSize())) {
            sb.append(" [ ").append(getSize()).append("Gb ]");
        }

        return sb.toString();
    }
}
