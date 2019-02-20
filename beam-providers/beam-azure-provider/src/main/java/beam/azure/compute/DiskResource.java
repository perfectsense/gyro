package beam.azure.compute;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
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

@ResourceName("disk")
public class DiskResource extends AzureResource {
    private String diskName;
    private String discId;
    private String resourceGroupName;
    private Integer size;
    private String osType;
    private String diskType;
    private String dataLoadSourceType;
    private String dataLoadSource;
    private Map<String, String> tags;

    public String getDiskName() {
        return diskName;
    }

    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }

    public String getDiscId() {
        return discId;
    }

    public void setDiscId(String discId) {
        this.discId = discId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @ResourceDiffProperty(updatable = true)
    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDiskType() {
        return diskType != null ? diskType.toUpperCase() : null;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }

    public String getDataLoadSourceType() {
        if (dataLoadSourceType == null) {
            dataLoadSourceType = "disk";
        }
        return dataLoadSourceType;
    }

    public void setDataLoadSourceType(String dataLoadSourceType) {
        this.dataLoadSourceType = dataLoadSourceType;
    }

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

        Disk disk = client.disks().getById(getDiscId());
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

        setDiscId(disk.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        Disk disk = client.disks().getById(getDiscId());

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

        client.disks().deleteById(getDiscId());
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
