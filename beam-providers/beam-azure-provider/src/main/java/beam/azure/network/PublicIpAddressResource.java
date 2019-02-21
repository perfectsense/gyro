package beam.azure.network;

import beam.azure.AzureResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPSkuType;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.Set;

@ResourceName("public-ip-address")
public class PublicIpAddressResource extends AzureResource {
    private String name;
    private String resourceGroupName;
    private Boolean skuBasic;
    private Boolean dynamic;
    private Integer idleTimeoutInMinute;
    private String publicIpAddressId;
    private String ipAddress;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public Boolean getSkuBasic() {
        if (skuBasic == null) {
            skuBasic = true;
        }

        return skuBasic;
    }

    public void setSkuBasic(Boolean skuBasic) {
        this.skuBasic = skuBasic;
    }

    public Boolean getDynamic() {
        if (dynamic == null) {
            dynamic = false;
        }

        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Integer getIdleTimeoutInMinute() {
        return idleTimeoutInMinute;
    }

    public void setIdleTimeoutInMinute(Integer idleTimeoutInMinute) {
        this.idleTimeoutInMinute = idleTimeoutInMinute;
    }

    public String getPublicIpAddressId() {
        return publicIpAddressId;
    }

    public void setPublicIpAddressId(String publicIpAddressid) {
        this.publicIpAddressId = publicIpAddressId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean refresh() {
        Azure client = createClient();

        PublicIPAddress publicIpAddress = client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getName());

        setIpAddress(publicIpAddress.ipAddress());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        PublicIPAddress publicIpAddress = client.publicIPAddresses()
            .define(getName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName())
            .withSku(getSkuBasic() ? PublicIPSkuType.BASIC : PublicIPSkuType.STANDARD)
            .withDynamicIP()
            .withIdleTimeoutInMinutes(getIdleTimeoutInMinute())
            .withoutLeafDomainLabel()
            .create();

        setPublicIpAddressId(publicIpAddress.id());
        setIpAddress(publicIpAddress.ipAddress());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.publicIPAddresses().deleteByResourceGroup(getResourceGroupName(), getName());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("public ip address");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(" - ").append(getName());
        }

        if (!ObjectUtils.isBlank(getIpAddress())) {
            sb.append(" - ").append(getIpAddress());
        }

        return sb.toString();
    }
}
