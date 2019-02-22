package beam.azure.network;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddress.DefinitionStages.WithCreate;
import com.microsoft.azure.management.network.PublicIPSkuType;
import com.microsoft.azure.management.resources.fluentcore.arm.AvailabilityZoneId;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
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
    private String availabilityZoneId;
    private String domainLabel;
    private Map<String, String> tags;

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

    @ResourceDiffProperty(updatable = true)
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

    public String getAvailabilityZoneId() {
        return availabilityZoneId;
    }

    public void setAvailabilityZoneId(String availabilityZoneId) {
        this.availabilityZoneId = availabilityZoneId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDomainLabel() {
        return domainLabel;
    }

    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
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

        PublicIPAddress publicIpAddress = client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getName());

        setIpAddress(publicIpAddress.ipAddress());
        setDomainLabel(publicIpAddress.leafDomainLabel());
        setIdleTimeoutInMinute(publicIpAddress.idleTimeoutInMinutes());
        setTags(publicIpAddress.tags());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        WithCreate withCreate = client.publicIPAddresses()
            .define(getName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName())
            .withSku(getSkuBasic() ? PublicIPSkuType.BASIC : PublicIPSkuType.STANDARD);

        if (!ObjectUtils.isBlank(getAvailabilityZoneId())) {
            withCreate = withCreate.withAvailabilityZone(AvailabilityZoneId.fromString(getAvailabilityZoneId()));
        }

        if (getSkuBasic()) {
            //basic
            withCreate = withCreate.withIdleTimeoutInMinutes(getIdleTimeoutInMinute());

            if (!ObjectUtils.isBlank(getDomainLabel())) {
                withCreate = withCreate.withLeafDomainLabel(getDomainLabel());
            } else {
                withCreate = withCreate.withoutLeafDomainLabel();
            }
        } else {
            //standard
            withCreate = withCreate.withStaticIP()
                .withIdleTimeoutInMinutes(getIdleTimeoutInMinute());

            if (!ObjectUtils.isBlank(getDomainLabel())) {
                withCreate = withCreate.withLeafDomainLabel(getDomainLabel());
            } else {
                withCreate = withCreate.withoutLeafDomainLabel();
            }
        }

        PublicIPAddress publicIpAddress = withCreate.withTags(getTags()).create();

        setPublicIpAddressId(publicIpAddress.id());
        setIpAddress(publicIpAddress.ipAddress());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        PublicIPAddress publicIPAddress = client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getName());

        PublicIPAddress.Update update = publicIPAddress.update();

        if (changedProperties.contains("idle-timeout-in-minute")) {
            update = update.withIdleTimeoutInMinutes(getIdleTimeoutInMinute());
        }

        if (changedProperties.contains("tags")) {
            update = update.withTags(getTags());
        }

        if (changedProperties.contains("domain-label")) {
            update = ObjectUtils.isBlank(getDomainLabel())
                ? update.withoutLeafDomainLabel() : update.withLeafDomainLabel(getDomainLabel());
        }

        if (!changedProperties.isEmpty()) {
            update.apply();
        }
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
