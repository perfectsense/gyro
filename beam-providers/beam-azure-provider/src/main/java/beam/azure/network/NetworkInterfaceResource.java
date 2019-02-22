package beam.azure.network;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates a network interface.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::network-interface network-interface-example
 *          network-interface-name: "network-interface-example"
 *          resource-group-name: $(azure::resource-group resource-group-network-interface-example | resource-group-name)
 *          network-id: $(azure::network network-example-interface | network-id)
 *          subnet: "subnet2"
 *          security-group-id: $(azure::network-security-group network-security-group-example-interface | network-security-group-id)
 *          tags: {
 *              Name: "network-interface-example"
 *          }
 *     end
 */
@ResourceName("network-interface")
public class NetworkInterfaceResource extends AzureResource {
    private String networkInterfaceName;
    private String resourceGroupName;
    private String networkId;
    private String subnet;
    private String staticIpAddress;
    private String securityGroupId;
    private String networkInterfaceId;
    private Map<String, String> tags;

    /**
     * Name of the network interface. (Required)
     */
    public String getNetworkInterfaceName() {
        return networkInterfaceName;
    }

    public void setNetworkInterfaceName(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
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
     * The id of the virtual network the interface is going be assigned with. (Required)
     */
    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * One of the subnet name from teh assigned virtual network. (Required)
     */
    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    /**
     * Choose to assign a static ip to the interface. Leave blank for dynamic ip.
     */
    public String getStaticIpAddress() {
        return staticIpAddress;
    }

    public void setStaticIpAddress(String staticIpAddress) {
        this.staticIpAddress = staticIpAddress;
    }

    /**
     * The id of a security group to be assigned with the interface.
     */
    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getNetworkInterfaceId() {
        return networkInterfaceId;
    }

    public void setNetworkInterfaceId(String networkInterfaceId) {
        this.networkInterfaceId = networkInterfaceId;
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

        NetworkInterface networkInterface = client.networkInterfaces().getByResourceGroup(getResourceGroupName(), getNetworkInterfaceName());

        setNetworkInterfaceName(networkInterface.name());
        setTags(networkInterface.tags());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        NetworkInterface.DefinitionStages.WithPrimaryPrivateIP withPrimaryPrivateIP = client.networkInterfaces()
            .define(getNetworkInterfaceName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName())
            .withExistingPrimaryNetwork(client.networks().getById(getNetworkId()))
            .withSubnet(getSubnet());

        NetworkInterface.DefinitionStages.WithCreate withCreate;

        if (!ObjectUtils.isBlank(getStaticIpAddress())) {
            withCreate = withPrimaryPrivateIP.withPrimaryPrivateIPAddressStatic(getStaticIpAddress());
        } else {
            withCreate = withPrimaryPrivateIP.withPrimaryPrivateIPAddressDynamic();
        }

        if (!ObjectUtils.isBlank(getSecurityGroupId())) {
            withCreate = withCreate.withExistingNetworkSecurityGroup(client.networkSecurityGroups().getById(getSecurityGroupId()));
        }

        NetworkInterface networkInterface = withCreate.withTags(getTags()).create();

        networkInterface.id();
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        NetworkInterface networkInterface = client.networkInterfaces().getByResourceGroup(getResourceGroupName(), getNetworkInterfaceName());

        networkInterface.update().withTags(getTags()).apply();
    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.networkInterfaces().deleteById(getNetworkInterfaceId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("network interface");

        if (!ObjectUtils.isBlank(getNetworkInterfaceName())) {
            sb.append(" - ").append(getNetworkInterfaceName());
        }

        if (!ObjectUtils.isBlank(getNetworkInterfaceId())) {
            sb.append(" - ").append(getNetworkInterfaceId());
        }

        return sb.toString();
    }
}
