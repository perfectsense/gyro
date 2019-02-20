package beam.azure.compute;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ResourceName("network")
public class NetworkResource extends AzureResource {
    private String name;
    private String resourceGroupName;
    private List<String> addressSpaces;
    private Map<String, String> subnets;
    private Map<String, String> tags;
    private String networkId;

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

    @ResourceDiffProperty(updatable = true)
    public List<String> getAddressSpaces() {
        if (addressSpaces == null) {
            addressSpaces = new ArrayList<>();
        }

        return addressSpaces;
    }

    public void setAddressSpaces(List<String> addressSpaces) {
        this.addressSpaces = addressSpaces;
    }

    public Map<String, String> getSubnets() {
        if (subnets == null) {
            subnets = new HashMap<>();
        }

        return subnets;
    }

    public void setSubnets(Map<String, String> subnets) {
        this.subnets = subnets;
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

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @Override
    public boolean refresh() {
        Azure client = createClient();

        Network network = client.networks().getById(getNetworkId());

        setTags(network.tags());
        setAddressSpaces(network.addressSpaces()); // change to list
        setName(network.name());

        getSubnets().clear();
        if (!network.subnets().isEmpty()) {
            for (String key : network.subnets().keySet()) {
                getSubnets().put(key, network.subnets().get(key).addressPrefix());
            }
        }

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        Network.DefinitionStages.WithCreate networkDefWithoutAddress = client.networks()
            .define(getName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName());

        Network.DefinitionStages.WithCreateAndSubnet withAddressSpace = null;

        for (String addressSpace : getAddressSpaces()) {
            withAddressSpace = networkDefWithoutAddress.withAddressSpace(addressSpace);
        }

        //other options

        Network network = withAddressSpace.withSubnets(getSubnets())
            .withTags(getTags())
            .create();

        network.addressSpaces();
        network.ddosProtectionPlanId();
        network.dnsServerIPs();
        network.isDdosProtectionEnabled();
        network.isVmProtectionEnabled();
        network.peerings();
        network.subnets();
        setNetworkId(network.id());

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        Network network = client.networks().getById(getNetworkId());

        Network.Update update = network.update();

        if (changedProperties.contains("address-spaces")) {
            NetworkResource oldResource = (NetworkResource) current;

            List<String> removeAddressSpaces = oldResource.getAddressSpaces().stream()
                .filter(((Predicate<String>) new HashSet<>(getAddressSpaces())::contains).negate())
                .collect(Collectors.toList());

            for (String addressSpace : removeAddressSpaces) {
                update = update.withoutAddressSpace(addressSpace);
            }

            List<String> addAddressSpaces = getAddressSpaces().stream()
                .filter(((Predicate<String>) new HashSet<>(oldResource.getAddressSpaces())::contains).negate())
                .collect(Collectors.toList());

            for (String addressSpace : addAddressSpaces) {
                update = update.withAddressSpace(addressSpace);
            }

            update.withSubnets(getSubnets());
        }

        if (changedProperties.contains("subnets")) {
            update = update.withSubnets(getSubnets());
        }

        if (changedProperties.contains("tags")) {
            update = update.withTags(getTags());
        }

        if (!changedProperties.isEmpty()) {
            update.apply();
        }

    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.networks().deleteById(getNetworkId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("network");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(" - ").append(getName());
        }

        if (!ObjectUtils.isBlank(getNetworkId())) {
            sb.append(" - ").append(getNetworkId());
        }

        return sb.toString();
    }
}
