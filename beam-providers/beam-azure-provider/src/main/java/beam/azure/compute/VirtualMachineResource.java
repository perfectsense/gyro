package beam.azure.compute;

import beam.azure.AzureResource;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import java.util.Set;

public class VirtualMachineResource extends AzureResource {
    private String name;
    private String resourceGroupId;
    private String adminUserName;
    private String adminPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        Azure client = createClient();

        client.virtualMachines()
            .define(getName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupId())
            .withNewPrimaryNetwork("10.0.0.0/28")
            .withPrimaryPrivateIPAddressDynamic()
            .withoutPrimaryPublicIPAddress()
            .withPopularWindowsImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER)
            .withAdminUsername(getAdminUserName())
            .withAdminPassword(getAdminPassword())
            .withNewDataDisk(10)
            .withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)
            .create();

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("virtual machine");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(" - ").append(getName());
        }

        if (!ObjectUtils.isBlank(getVirtualMachineId())) {
            sb.append(" - ").append(getVirtualMachineId());
        }

        return sb.toString();
    }
}
