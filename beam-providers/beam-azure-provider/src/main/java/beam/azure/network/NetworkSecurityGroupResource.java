package beam.azure.network;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.psddev.dari.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates a network security group.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::network-security-group network-security-group-example
 *          network-security-group-name: "network-security-group-example"
 *          resource-group-name: $(azure::resource-group resource-group-network-security-group-example | resource-group-name)
 *
 *          tags: {
 *              Name: "network-security-group-example"
 *          }
 *     end
 */
@ResourceName("network-security-group")
public class NetworkSecurityGroupResource extends AzureResource {
    private String networkSecurityGroupName;
    private String resourceGroupName;
    private String networkSecurityGroupId;
    private Map<String, String> tags;

    /**
     * Name of the security group. (Required)
     */
    public String getNetworkSecurityGroupName() {
        return networkSecurityGroupName;
    }

    public void setNetworkSecurityGroupName(String networkSecurityGroupName) {
        this.networkSecurityGroupName = networkSecurityGroupName;
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

    public String getNetworkSecurityGroupId() {
        return networkSecurityGroupId;
    }

    public void setNetworkSecurityGroupId(String networkSecurityGroupId) {
        this.networkSecurityGroupId = networkSecurityGroupId;
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

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups().getById(getNetworkSecurityGroupId());

        setNetworkSecurityGroupName(networkSecurityGroup.name());
        setTags(networkSecurityGroup.tags());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups()
            .define(getNetworkSecurityGroupName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName())
            .withTags(getTags())
            .create();

        setNetworkSecurityGroupId(networkSecurityGroup.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups().getById(getNetworkSecurityGroupId());

        networkSecurityGroup.update().withTags(getTags()).apply();
    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.networkSecurityGroups().deleteById(getNetworkSecurityGroupId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("network security group");

        if (!ObjectUtils.isBlank(getNetworkSecurityGroupName())) {
            sb.append(" - ").append(getNetworkSecurityGroupName());
        }

        if (!ObjectUtils.isBlank(getNetworkSecurityGroupId())) {
            sb.append(" - ").append(getNetworkSecurityGroupId());
        }

        return sb.toString();
    }
}
