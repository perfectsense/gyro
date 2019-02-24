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

/**
 * Creates a public ip address.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::public-ip-address public-ip-address-example
 *          name: "public-ip-address-example"
 *          resource-group-name: $(azure::resource-group resource-group-public-ip-address-example | resource-group-name)
 *          idle-timeout-in-minute: 4
 *          sku-basic: false
 *
 *          tags: {
 *              Name: "public-ip-address-example"
 *          }
 *     end
 */
@ResourceName("public-ip-address")
public class PublicIpAddressResource extends AzureResource {
    private String publicIpAddressName;
    private String resourceGroupName;
    private Boolean skuBasic;
    private Boolean dynamic;
    private Integer idleTimeoutInMinute;
    private String publicIpAddressId;
    private String ipAddress;
    private String availabilityZoneId;
    private String domainLabel;
    private Map<String, String> tags;

    /**
     * Name of the public ip address. (Required)
     */
    public String getPublicIpAddressName() {
        return publicIpAddressName;
    }

    public void setPublicIpAddressName(String publicIpAddressName) {
        this.publicIpAddressName = publicIpAddressName;
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
     * Specify if Sku type is basic or standard. Defaults to true.
     */
    public Boolean getSkuBasic() {
        if (skuBasic == null) {
            skuBasic = true;
        }

        return skuBasic;
    }

    public void setSkuBasic(Boolean skuBasic) {
        this.skuBasic = skuBasic;
    }

    /**
     * Specify if using dynamic ip or not. Defaults to false.
     */
    public Boolean getDynamic() {
        if (dynamic == null) {
            dynamic = false;
        }

        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Specify the idle time in minutes before time out. Valid values [ Integer from 4 - 30]. (Required)
     */
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

    /**
     * Specify the availability zone.
     */
    public String getAvailabilityZoneId() {
        return availabilityZoneId;
    }

    public void setAvailabilityZoneId(String availabilityZoneId) {
        this.availabilityZoneId = availabilityZoneId;
    }

    /**
     * Specify the domain prefix.
     */
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

        PublicIPAddress publicIpAddress = client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getPublicIpAddressName());

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
            .define(getPublicIpAddressName())
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

        PublicIPAddress publicIpAddress = client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getPublicIpAddressName());

        PublicIPAddress.Update update = publicIpAddress.update();

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

        client.publicIPAddresses().deleteByResourceGroup(getResourceGroupName(), getPublicIpAddressName());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("public ip address");

        if (!ObjectUtils.isBlank(getPublicIpAddressName())) {
            sb.append(" - ").append(getPublicIpAddressName());
        }

        if (!ObjectUtils.isBlank(getIpAddress())) {
            sb.append(" - ").append(getIpAddress());
        }

        return sb.toString();
    }
}
