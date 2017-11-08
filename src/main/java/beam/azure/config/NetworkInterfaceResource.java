package beam.azure.config;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.network.NetworkInterfaceOperations;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.PublicIpAddressOperations;
import com.microsoft.azure.management.network.models.NetworkInterface;
import com.microsoft.azure.management.network.models.NetworkInterfaceIpConfiguration;
import com.microsoft.azure.management.network.models.PublicIpAddress;
import com.microsoft.azure.management.network.models.ResourceId;
import com.psddev.dari.util.CompactMap;

import java.util.*;

public class NetworkInterfaceResource extends AzureResource<NetworkInterface> {
    private String name;
    private String privateIp;
    private String publicIp;
    private String publicIpAllocation;
    private Map<String, String> tags;
    private boolean dynamic;
    private BeamReference virtualMachine;
    private String subnetId;
    private String id;
    private String publicIpId;
    private String loadBalancerName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIpId() {
        return publicIpId;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPublicIpAllocation() {
        return publicIpAllocation;
    }

    public void setPublicIpAllocation(String publicIpAllocation) {
        this.publicIpAllocation = publicIpAllocation;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public BeamReference getVirtualMachine() {
        return virtualMachine;
    }

    public void setVirtualMachine(BeamReference virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getResourceGroup(), getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, NetworkInterface nic) {
        setName(nic.getName());
        setId(nic.getId());
        setTags(nic.getTags());

        NetworkInterfaceIpConfiguration configuration = nic.getIpConfigurations().get(0);
        setSubnetId(configuration.getSubnet().getId());

        if (!nic.getIpConfigurations().get(0).getLoadBalancerBackendAddressPools().isEmpty()) {
            String backendPoolId = nic.getIpConfigurations().get(0).getLoadBalancerBackendAddressPools().get(0).getId();
            String[] idParts = backendPoolId.split("/");
            String loadBalancerName = idParts[idParts.length - 3];
            setLoadBalancerName(loadBalancerName);
        }

        setDynamic("Dynamic".equals(configuration.getPrivateIpAllocationMethod()));
        setPrivateIp(configuration.getPrivateIpAddress());

        if (configuration.getPublicIpAddress() == null) {
            setPublicIpAllocation("None");
            setPublicIp(null);
        } else {
            String publicIpId = configuration.getPublicIpAddress().getId();
            setPublicIpId(publicIpId);
            String[] idParts = publicIpId.split("/");
            String publicIpName = idParts[idParts.length-1];

            PublicIpAddress address = findPublicIpfromName(cloud, publicIpName);
            setPublicIpAllocation(address.getPublicIpAllocationMethod());
            setPublicIp(address.getIpAddress());
        }
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkInterfaceOperations nIOperations = client.getNetworkInterfacesOperations();
        NetworkInterface nic = new NetworkInterface();
        nic.setName(getName());
        nic.setLocation(getRegion());

        NetworkInterfaceIpConfiguration configuration = new NetworkInterfaceIpConfiguration();
        ArrayList<NetworkInterfaceIpConfiguration> configurations = new ArrayList<>();
        configurations.add(configuration);
        nic.setIpConfigurations(configurations);

        configuration.setName("ipconfig");
        ResourceId subnetId = new ResourceId();
        subnetId.setId(getSubnetId());
        configuration.setSubnet(subnetId);

        if (getLoadBalancerName() != null) {
            LoadBalancerResource loadBalancerResource = new LoadBalancerResource();
            loadBalancerResource.setName(getLoadBalancerName());
            loadBalancerResource.setRegion(getRegion());
            String backPoolId = loadBalancerResource.getBackendPoolId(cloud, "backendpool");
            ResourceId resourceId = new ResourceId();
            resourceId.setId(backPoolId);
            ArrayList<ResourceId> resourceIds = new ArrayList<>();
            resourceIds.add(resourceId);
            configuration.setLoadBalancerBackendAddressPools(resourceIds);
        }

        if (isDynamic()) {
            configuration.setPrivateIpAllocationMethod("Dynamic");
        } else {
            configuration.setPrivateIpAllocationMethod("Static");
            configuration.setPrivateIpAddress(getPrivateIp());
        }

        if (!"None".equals(getPublicIpAllocation()) && getPublicIpId() != null) {
            String addressId = getPublicIpId();
            ResourceId id = new ResourceId();
            id.setId(addressId);
            configuration.setPublicIpAddress(id);
        }

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        nic.setTags(tags);

        try {
            nIOperations.createOrUpdate(getResourceGroup(), getName(), nic);
            NetworkInterface self = nIOperations.get(getResourceGroup(), getName()).getNetworkInterface();
            setId(self.getId());

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update network interface: " + getName());
        }

        if (!"None".equals(getPublicIpAllocation())) {
            String addressId = allocatePublicIp(cloud);
            ResourceId id = new ResourceId();
            id.setId(addressId);
            configuration.setPublicIpAddress(id);
        }

        try {
            nIOperations.createOrUpdate(getResourceGroup(), getName(), nic);
            NetworkInterface self = nIOperations.get(getResourceGroup(), getName()).getNetworkInterface();
            setId(self.getId());
            configuration = self.getIpConfigurations().get(0);
            setPrivateIp(configuration.getPrivateIpAddress());

            if (configuration.getPublicIpAddress() == null) {
                setPublicIp(null);
            } else {
                String publicIpId = configuration.getPublicIpAddress().getId();
                setPublicIpId(publicIpId);
                String[] idParts = publicIpId.split("/");
                String publicIpName = idParts[idParts.length - 1];
                PublicIpAddress address = findPublicIpfromName(cloud, publicIpName);
                setPublicIp(address.getIpAddress());
            }

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update network interface: " + getName());
        }

        if (!"Dynamic".equals(getPublicIpAllocation())) {
            releasePublicIp(cloud);
        }

        VirtualMachineResource virtualMachineResource = (VirtualMachineResource)getVirtualMachine().resolve();
        virtualMachineResource.setPublicIpAddress(getPublicIp());
        virtualMachineResource.setPrivateIpAddress(getPrivateIp());
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, NetworkInterface> current, Set<String> changedProperties) {
        if (((NetworkInterfaceResource) current).getTags().containsKey("beam.verifying")) {
            getTags().put("beam.verifying", "true");
            if ("None".equals(getPublicIpAllocation())) {
                setPublicIpAllocation("Dynamic");
                changedProperties.remove("publicIpAllocation");
            }
        }

        setSubnetId(((NetworkInterfaceResource) current).getSubnetId());
        setLoadBalancerName(((NetworkInterfaceResource) current).getLoadBalancerName());
        if (changedProperties.contains("publicIpAllocation") || changedProperties.contains("publicIp")) {
            setPublicIpId(null);
        }

        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkInterfaceOperations nIOperations = client.getNetworkInterfacesOperations();

        try {
            nIOperations.delete(getResourceGroup(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete network interface: " + getName());
        }

        releasePublicIp(cloud);
    }

    public String allocatePublicIp(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        PublicIpAddress publicIpAddress;
        String addressId = null;

        try {
            if ("Dynamic".equals(getPublicIpAllocation())) {
                publicIpAddress = new PublicIpAddress();
                publicIpAddress.setPublicIpAllocationMethod(getPublicIpAllocation());
                publicIpAddress.setLocation(getRegion());
                publicIpAddress.setName(getName());

                pIAOperations.createOrUpdate(getResourceGroup(), getName(), publicIpAddress);
                addressId = pIAOperations.get(getResourceGroup(), getName()).getPublicIpAddress().getId();

            } else {
                if (getPublicIp() == null) {
                    publicIpAddress = new PublicIpAddress();
                    publicIpAddress.setPublicIpAllocationMethod(getPublicIpAllocation());
                    publicIpAddress.setLocation(getRegion());

                    String name = BeamRuntime.getCurrentRuntime().getProject() + "-" + UUID.randomUUID().toString();
                    publicIpAddress.setName(name);

                    pIAOperations.createOrUpdate(getResourceGroup(), name, publicIpAddress);
                    addressId = pIAOperations.get(getResourceGroup(), name).getPublicIpAddress().getId();
                } else {
                    publicIpAddress = findPublicIpfromAddress(cloud, getPublicIp());

                    pIAOperations.createOrUpdate(getResourceGroup(), publicIpAddress.getName(), publicIpAddress);
                    addressId = pIAOperations.get(getResourceGroup(), publicIpAddress.getName()).getPublicIpAddress().getId();
                }
            }
        } catch (Exception error) {
            this.delete(cloud);
            error.printStackTrace();
            throw new BeamException("Fail to allocate public ip address!");
        }

        return addressId;
    }

    public void releasePublicIp(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        try {
            if (pIAOperations.get(getResourceGroup(),getName()).getPublicIpAddress() != null) {
                pIAOperations.delete(getResourceGroup(), getName());
            }

        } catch (Exception error) {
        }
    }

    public PublicIpAddress findPublicIpfromName(AzureCloud cloud, String name) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        try {
            return pIAOperations.get(getResourceGroup(), name).getPublicIpAddress();

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to find public ip address: " + name);
        }
    }

    private PublicIpAddress findPublicIpfromAddress(AzureCloud cloud, String publicIpAddress) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        try {
            for (PublicIpAddress address : pIAOperations.list(getResourceGroup()).getPublicIpAddresses()) {
                if (publicIpAddress.equals(address.getIpAddress())) {
                    return address;
                }
            }

            return null;

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to find public ip address: " + publicIpAddress);
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        VirtualMachineResource virtualMachineResource = (VirtualMachineResource)getVirtualMachine().resolve();

        sb.append("network interface");
        sb.append(' ');
        sb.append(virtualMachineResource.getName());

        return sb.toString();
    }
}
