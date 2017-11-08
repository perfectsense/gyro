package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.BeamRuntime;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.compute.AvailabilitySetOperations;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.AvailabilitySet;
import com.microsoft.azure.management.compute.models.LinuxConfiguration;
import com.microsoft.azure.management.compute.models.OSProfile;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.network.models.*;
import com.psddev.dari.util.CompactMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VirtualNetworkResource extends AzureResource<VirtualNetwork> {

    private Set<SecurityGroupResource> securityGroups;
    private String id;
    private String name;
    private String cidrBlock;
    private Map<String, String> tags;
    private Set<SubnetResource> subnets;
    private Set<LoadBalancerResource> loadBalancers;
    private Set<ApplicationGatewayResource> applicationGateways;
    private Set<AvailabilitySetResource> availabilitySets;
    private Set<AzureGroupResource> azureGroupResources;
    private Set<KeyPairResource> keyPairs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Set<SecurityGroupResource> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new HashSet<>();
        }
        return securityGroups;
    }

    public void setSecurityGroups(Set<SecurityGroupResource> securityGroups) {
        this.securityGroups = securityGroups;
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

    public Set<SubnetResource> getSubnets() {
        if (subnets == null) {
            subnets = new HashSet<>();
        }
        return subnets;
    }

    public void setSubnets(Set<SubnetResource> subnets) {
        this.subnets = subnets;
    }

    public Set<LoadBalancerResource> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new HashSet<>();
        }
        return loadBalancers;
    }

    public void setLoadBalancers(Set<LoadBalancerResource> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public Set<ApplicationGatewayResource> getApplicationGateways() {
        if (applicationGateways == null) {
            applicationGateways = new HashSet<>();
        }
        return applicationGateways;
    }

    public void setApplicationGateways(Set<ApplicationGatewayResource> applicationGateways) {
        this.applicationGateways = applicationGateways;
    }

    public Set<AvailabilitySetResource> getAvailabilitySets() {
        if (availabilitySets == null) {
            availabilitySets = new HashSet<>();
        }
        return availabilitySets;
    }

    public void setAvailabilitySets(Set<AvailabilitySetResource> availabilitySets) {
        this.availabilitySets = availabilitySets;
    }

    public Set<AzureGroupResource> getAzureGroupResources() {
        if (azureGroupResources == null) {
            azureGroupResources = new HashSet<>();
        }
        return azureGroupResources;
    }

    public void setAzureGroupResources(Set<AzureGroupResource> azureGroupResources) {
        this.azureGroupResources = azureGroupResources;
    }

    public Set<KeyPairResource> getKeyPairs() {
        if (keyPairs == null) {
            keyPairs = new HashSet<>();
        }
        return keyPairs;
    }

    public void setKeyPairs(Set<KeyPairResource> keyPairs) {
        this.keyPairs = keyPairs;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getResourceGroup(), getClass().getName(), getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, VirtualNetwork vn) {
        setName(vn.getName());
        String cidr;
        try {
            cidr = vn.getAddressSpace().getAddressPrefixes().get(0);
        } catch (Exception error) {
            throw new BeamException("Fail to load virtual network cidrBlock!");
        }

        setCidrBlock(cidr);
        setTags(vn.getTags());

        NetworkResourceProviderClient nRPClient = cloud.createNetworkManagementClient();
        NetworkSecurityGroupOperations nSGoperations = nRPClient.getNetworkSecurityGroupsOperations();

        try {
            for (NetworkSecurityGroup group : nSGoperations.list(getResourceGroup()).getNetworkSecurityGroups()) {
                SecurityGroupResource sgResource = new SecurityGroupResource();
                sgResource.setRegion(getRegion());
                sgResource.init(cloud, filter, group);
                getSecurityGroups().add(sgResource);
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load security groups!");
        }

        Map<String, SubnetResource> subnetResourceById = new HashMap<>();
        for (Subnet subnet : vn.getSubnets()) {
            SubnetResource subnetResource = new SubnetResource();
            subnetResource.setRegion(getRegion());
            subnetResource.setVnet(subnetResource.newReference(this));
            subnetResource.init(cloud, filter, subnet);
            getSubnets().add(subnetResource);
            subnetResourceById.put(subnetResource.getId(), subnetResource);
        }

        ComputeManagementClient client = cloud.createComputeManagementClient();
        AvailabilitySetOperations aSOperations = client.getAvailabilitySetsOperations();
        Map<String, AvailabilitySetResource> availabilitySetResourceMap = new HashMap<>();

        try {
            for (AvailabilitySet as : aSOperations.list(getResourceGroup()).getAvailabilitySets()) {
                AvailabilitySetResource availabilitySetResource = new AvailabilitySetResource();
                availabilitySetResource.setRegion(getRegion());
                availabilitySetResource.init(cloud, filter, as);
                getAvailabilitySets().add(availabilitySetResource);
                availabilitySetResourceMap.put(availabilitySetResource.getName(), availabilitySetResource);
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load availabilitySets!");
        }

        LoadBalancerOperations lBOperations = nRPClient.getLoadBalancersOperations();

        try {
            for (LoadBalancer elb : lBOperations.list(getResourceGroup()).getLoadBalancers()) {
                LoadBalancerResource loadBalancerResource = new LoadBalancerResource();
                loadBalancerResource.setRegion(getRegion());
                loadBalancerResource.init(cloud, filter, elb);
                getLoadBalancers().add(loadBalancerResource);

                AvailabilitySetResource availabilitySetResource = availabilitySetResourceMap.get(loadBalancerResource.getName());
                loadBalancerResource.setAvailabilitySet(loadBalancerResource.newReference(availabilitySetResource));
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load loadBalancers!");
        }

        ApplicationGatewayOperations aGOperations = nRPClient.getApplicationGatewaysOperations();

        try {
            for (ApplicationGateway applicationGateway : aGOperations.list(getResourceGroup()).getApplicationGateways()) {
                ApplicationGatewayResource applicationGatewayResource = new ApplicationGatewayResource();
                applicationGatewayResource.setRegion(getRegion());
                applicationGatewayResource.init(cloud, filter, applicationGateway);
                getApplicationGateways().add(applicationGatewayResource);

                for (LoadBalancerResource loadBalancerResource : getLoadBalancers()) {
                    if (loadBalancerResource.getName().equals(applicationGatewayResource.getName())) {
                        applicationGatewayResource.setLoadBalancer(applicationGatewayResource.newReference(loadBalancerResource));
                    }
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load application gateways!");
        }


        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();

        VirtualMachine virtualMachine = null;

        try {
            Set<String> groupNames = new HashSet<>();
            for (VirtualMachine vm : vMOperations.list(getResourceGroup()).getVirtualMachines()) {
                if (isInclude(filter, vm) && vm.getTags().get("group") != null) {
                    groupNames.add(vm.getTags().get("group"));
                }

                virtualMachine = vm;
            }

            for (String groupName : groupNames) {
                AzureGroupResource group = new AzureGroupResource();
                group.setRegion(getRegion());
                group.setName(groupName);
                group.init(cloud, filter, null);
                getAzureGroupResources().add(group);
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load azure groups!");
        }

        if (virtualMachine != null) {
            OSProfile osProfile = virtualMachine.getOSProfile();
            LinuxConfiguration configuration = osProfile.getLinuxConfiguration();
            String publicKey = configuration.getSshConfiguration().getPublicKeys().get(0).getKeyData();

            KeyPairResource keyPairResource = new KeyPairResource();
            keyPairResource.setRegion(getRegion());
            keyPairResource.init(cloud, filter, null);
            keyPairResource.setPublicKey(publicKey);

            getKeyPairs().add(keyPairResource);

        } else {
            String keyName = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), getRegion());
            String pubPath = System.getProperty("user.home") + "/.ssh/" + keyName + ".pub";

            try {
                File pubFile = new File(pubPath);
                if (pubFile.exists()) {
                    String publicKey = new Scanner(new File(pubPath)).useDelimiter("\\Z").next();
                    KeyPairResource keyPairResource = new KeyPairResource();
                    keyPairResource.setRegion(getRegion());
                    keyPairResource.init(cloud, filter, null);
                    keyPairResource.setPublicKey(publicKey);

                    getKeyPairs().add(keyPairResource);
                }

            } catch (IOException error) {
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getKeyPairs());
        create.create(getSecurityGroups());
        create.create(getSubnets());
        create.create(getAvailabilitySets());
        create.create(getLoadBalancers());
        create.create(getApplicationGateways());
        create.create(getAzureGroupResources());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, VirtualNetwork> current) throws Exception {
        update.update(((VirtualNetworkResource)current).getKeyPairs(), getKeyPairs());
        update.update(((VirtualNetworkResource)current).getSecurityGroups(), getSecurityGroups());
        update.update(((VirtualNetworkResource)current).getSubnets(), getSubnets());
        update.update(((VirtualNetworkResource)current).getAvailabilitySets(), getAvailabilitySets());
        update.update(((VirtualNetworkResource)current).getLoadBalancers(), getLoadBalancers());
        update.update(((VirtualNetworkResource)current).getApplicationGateways(), getApplicationGateways());
        update.update(((VirtualNetworkResource)current).getAzureGroupResources(), getAzureGroupResources());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getAzureGroupResources());
        delete.delete(getApplicationGateways());
        delete.delete(getLoadBalancers());
        delete.delete(getAvailabilitySets());
        delete.delete(getSubnets());
        delete.delete(getSecurityGroups());
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        VirtualNetworkOperations vNOperations = client.getVirtualNetworksOperations();
        VirtualNetwork vn = new VirtualNetwork();
        vn.setName(getName());
        vn.setLocation(getRegion());

        AddressSpace addressSpace = new AddressSpace();
        addressSpace.getAddressPrefixes().add(getCidrBlock());
        vn.setAddressSpace(addressSpace);

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        vn.setTags(tags);

        try {
            vNOperations.createOrUpdate(getResourceGroup(), getName(), vn);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update virtual network: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, VirtualNetwork> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        VirtualNetworkOperations vNOperations = client.getVirtualNetworksOperations();

        try {
            vNOperations.beginDeleting(getResourceGroup(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete virtual network: " + getName());
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String vnName = getName();
        String cidrBlock = getCidrBlock();
        sb.append("virtual network ");
        sb.append(vnName);
        sb.append(' ');
        sb.append(cidrBlock);

        return sb.toString();
    }
}
