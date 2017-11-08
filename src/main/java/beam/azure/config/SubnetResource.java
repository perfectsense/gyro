package beam.azure.config;

import java.util.*;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.google.common.base.Joiner;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.SubnetOperations;
import com.microsoft.azure.management.network.models.ResourceId;
import com.microsoft.azure.management.network.models.Subnet;

public class SubnetResource extends AzureResource<Subnet> {

    private String name;
    private String id;
    private String cidrBlock;
    private BeamReference vnet;
    private Set<VirtualMachineResource> virtualMachines;
    private String securityGroup;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    @ResourceDiffProperty(updatable = true)
    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BeamReference getVnet() {
        return newParentReference(VirtualNetworkResource.class, vnet);
    }

    public void setVnet(BeamReference vnet) {
        this.vnet = vnet;
    }

    public Set<VirtualMachineResource> getVirtualMachines() {
        if (virtualMachines == null) {
            virtualMachines = new HashSet<>();
        }
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VirtualMachineResource> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        VirtualNetworkResource virtualNetworkResource = (VirtualNetworkResource)getVnet().resolve();
        return Arrays.asList(Joiner.
                on(',').
                useForNull("null").
                join(Arrays.asList(
                        virtualNetworkResource.getName(),
                        getRegion(),
                        getName())));
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, Subnet subnet) {
        setName(subnet.getName());
        setId(subnet.getId());
        setCidrBlock(subnet.getAddressPrefix());

        if (subnet.getNetworkSecurityGroup() != null) {
            String securityGroupId = subnet.getNetworkSecurityGroup().getId();
            String[] uriParts = securityGroupId.split("/");
            String securityGroupName = uriParts[uriParts.length-1];
            setSecurityGroup(securityGroupName);
        }

        ComputeManagementClient client = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();

        Map<String, List<VirtualMachineResource>> vmByLayerName = new HashMap<>();

        try {
            for (VirtualMachine vm : vMOperations.list(getResourceGroup()).getVirtualMachines()) {
                if (!isInclude(filter, vm) || vm.getTags().get("group") != null) {
                    continue;
                }

                String layerName = vm.getTags().get("beam.layer");
                List<VirtualMachineResource> virtualMachines = vmByLayerName.get(layerName);
                if (virtualMachines == null) {
                    virtualMachines = new ArrayList<>();
                    vmByLayerName.put(layerName, virtualMachines);
                }

                String nic = vm.getNetworkProfile().getNetworkInterfaces().get(0).getReferenceUri();
                for (ResourceId ipConfig : subnet.getIpConfigurations()) {
                    if (ipConfig.getId().contains(nic)) {
                        VirtualMachineResource virtualMachineResource = new VirtualMachineResource();
                        virtualMachineResource.setRegion(getRegion());
                        virtualMachineResource.init(cloud, filter, vm);

                        getVirtualMachines().add(virtualMachineResource);
                        virtualMachines.add(virtualMachineResource);
                    }
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load subnet virtual machines! ");
        }

        for (List<VirtualMachineResource> virtualMachines : vmByLayerName.values()) {
            // Sort instances by launch date.
            Collections.sort(virtualMachines, new Comparator<VirtualMachineResource>() {
                @Override
                public int compare(VirtualMachineResource o1, VirtualMachineResource o2) {
                    String[] o1Parts = o1.getName().split("-");
                    String o1DateString = o1Parts[o1Parts.length-1];

                    String[] o2Parts = o2.getName().split("-");
                    String o2DateString = o2Parts[o2Parts.length-1];

                    return o1DateString.compareTo(o2DateString);
                }
            });

            Integer beamLaunchIndex = 0;
            for (VirtualMachineResource virtualMachine : virtualMachines) {
                virtualMachine.setBeamLaunchIndex(beamLaunchIndex++);
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getVirtualMachines());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, Subnet> current) throws Exception {
        SubnetResource currentSubnet = (SubnetResource) current;
        update.update(currentSubnet.getVirtualMachines(), getVirtualMachines());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getVirtualMachines());
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        SubnetOperations subnetOperations = client.getSubnetsOperations();
        Subnet subnet = new Subnet();
        subnet.setName(getName());
        subnet.setAddressPrefix(getCidrBlock());

        ResourceId securityGroupId = new ResourceId();
        securityGroupId.setId(getSecurityGroupId(cloud, getSecurityGroup()));
        subnet.setNetworkSecurityGroup(securityGroupId);

        VirtualNetworkResource virtualNetworkResource = (VirtualNetworkResource)getVnet().resolve();
        try {
            subnetOperations.createOrUpdate(getResourceGroup(), virtualNetworkResource.getName(), getName(), subnet);
            setId(subnetOperations.get(getResourceGroup(), virtualNetworkResource.getName(), getName()).getSubnet().getId());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update subnet: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, Subnet> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        SubnetOperations subnetOperations = client.getSubnetsOperations();

        VirtualNetworkResource virtualNetworkResource = (VirtualNetworkResource)getVnet().resolve();
        try {
            subnetOperations.delete(getResourceGroup(), virtualNetworkResource.getName(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete subnet: " + getName());
        }
    }

    private String getSecurityGroupId(AzureCloud cloud, String name) {
        return String.format("%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/networkSecurityGroups/", name);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("subnet");

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(getCidrBlock());
        }

        return sb.toString();
    }
}
