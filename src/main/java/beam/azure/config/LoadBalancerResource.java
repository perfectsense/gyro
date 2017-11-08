package beam.azure.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.network.LoadBalancerOperations;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.PublicIpAddressOperations;
import com.microsoft.azure.management.network.models.*;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.util.*;

public class LoadBalancerResource extends AzureResource<LoadBalancer> {

    private Integer loadBalancerId;
    private String name;
    private List<AzureLoadBalancerListenerConfig> listeners;
    private List<String> hostnames;
    private List<String> verificationHostnames;
    private int idleTimeout;
    private String publicIp;
    private String publicIpId;
    private String probeProtocol;
    private String probePath;
    private int probeInterval;
    private int numberOfProbes;
    private int probePort;
    private Map<String, String> tags;
    private BeamReference availabilitySet;
    private BeamReference subnet;
    private String privateIp;

    public Integer getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(Integer loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AzureLoadBalancerListenerConfig> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        return listeners;
    }

    public void setListeners(List<AzureLoadBalancerListenerConfig> listeners) {
        this.listeners = listeners;
    }

    @ResourceDiffProperty(updatable = true)
    public String getListenersHash() {
        String configHash = StringUtils.hex(StringUtils.md5(ObjectUtils.toJson(getListeners())));
        return configHash;
    }

    public List<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new ArrayList<>();
        }

        return hostnames;
    }

    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public List<String> getVerificationHostnames() {
        if (verificationHostnames == null) {
            verificationHostnames = new ArrayList<>();
        }

        return verificationHostnames;
    }

    public void setVerificationHostnames(List<String> verificationHostnames) {
        this.verificationHostnames = verificationHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    @ResourceDiffProperty(updatable = true)
    public String getProbeProtocol() {
        return probeProtocol;
    }

    public void setProbeProtocol(String probeProtocol) {
        this.probeProtocol = probeProtocol;
    }

    @ResourceDiffProperty(updatable = true)
    public String getProbePath() {
        return probePath;
    }

    public void setProbePath(String probePath) {
        this.probePath = probePath;
    }

    @ResourceDiffProperty(updatable = true)
    public int getProbeInterval() {
        return probeInterval;
    }

    public void setProbeInterval(int probeInterval) {
        this.probeInterval = probeInterval;
    }

    @ResourceDiffProperty(updatable = true)
    public int getNumberOfProbes() {
        return numberOfProbes;
    }

    public void setNumberOfProbes(int numberOfProbes) {
        this.numberOfProbes = numberOfProbes;
    }

    @ResourceDiffProperty(updatable = true)
    public int getProbePort() {
        return probePort;
    }

    public void setProbePort(int probePort) {
        this.probePort = probePort;
    }

    public BeamReference getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(BeamReference availabilitySet) {
        this.availabilitySet = availabilitySet;
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

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPublicIpId() {
        return publicIpId;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    public BeamReference getSubnet() {
        return subnet;
    }

    public void setSubnet(BeamReference subnet) {
        this.subnet = subnet;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }


    @Override
    public String awsId() {
        return getName();
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, LoadBalancer loadBalancer) {
        setName(loadBalancer.getName());
        for (LoadBalancingRule rule : loadBalancer.getLoadBalancingRules()) {
            AzureLoadBalancerListenerConfig listener = new AzureLoadBalancerListenerConfig();
            listener.setDestPort(rule.getBackendPort());
            listener.setSourcePort(rule.getFrontendPort());
            listener.setProtocol(rule.getProtocol().toUpperCase());
            getListeners().add(listener);
            setIdleTimeout(rule.getIdleTimeoutInMinutes());
        }

        Probe probe = loadBalancer.getProbes().get(0);
        setProbeProtocol(probe.getProtocol().toUpperCase());
        setProbeInterval(probe.getIntervalInSeconds());
        setProbePath(probe.getRequestPath());
        setNumberOfProbes(probe.getNumberOfProbes());
        setProbePort(probe.getPort());

        FrontendIpConfiguration frontendIpConfiguration = loadBalancer.getFrontendIpConfigurations().get(0);
        if (frontendIpConfiguration.getPublicIpAddress() != null) {
            String publicIpId = loadBalancer.getFrontendIpConfigurations().get(0).getPublicIpAddress().getId();
            setPublicIpId(publicIpId);
            setPublicIp(getPublicIpFromId(cloud, publicIpId));
        } else if (frontendIpConfiguration.getSubnet() != null) {
            String subnetId = frontendIpConfiguration.getSubnet().getId();
            setSubnet(newReference(SubnetResource.class, subnetId));
            setPrivateIp(frontendIpConfiguration.getPrivateIpAddress());
        }

        setTags(loadBalancer.getTags());
    }

    public String getPublicIpFromId(AzureCloud cloud, String publicIpId) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        String publicIp = null;
        try {
            String[] idParts = publicIpId.split("/");
            String publicIpName = idParts[idParts.length-1];
            PublicIpAddress address = pIAOperations.get(getResourceGroup(), publicIpName).getPublicIpAddress();
            publicIp = address.getIpAddress();

        } catch (Exception error) {
        }

        return publicIp;
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        LoadBalancerOperations lBOperations = client.getLoadBalancersOperations();

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setName(getName());
        loadBalancer.setLocation(getRegion());


        FrontendIpConfiguration frontendIpConfiguration = new FrontendIpConfiguration();
        frontendIpConfiguration.setName("frontendipconfig");

        if (getSubnet() == null) {
            if (getPublicIpId() == null) {
                NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
                networkInterfaceResource.setRegion(getRegion());
                networkInterfaceResource.setName(getName());
                networkInterfaceResource.setPublicIpAllocation("Static");
                String publicIpId = networkInterfaceResource.allocatePublicIp(cloud);
                setPublicIpId(publicIpId);
            }

            setPublicIp(getPublicIpFromId(cloud, getPublicIpId()));
            ResourceId ipId = new ResourceId();
            ipId.setId(getPublicIpId());
            frontendIpConfiguration.setPublicIpAddress(ipId);
        }

        if (getSubnet() != null) {
            ResourceId subnetId = new ResourceId();
            SubnetResource subnetResource = (SubnetResource)getSubnet().resolve();
            subnetId.setId(subnetResource.getId());
            frontendIpConfiguration.setSubnet(subnetId);
        }

        ArrayList<FrontendIpConfiguration> frontendIpConfigurations = new ArrayList<>();
        frontendIpConfigurations.add(frontendIpConfiguration);
        loadBalancer.setFrontendIpConfigurations(frontendIpConfigurations);

        BackendAddressPool backendAddressPool = new BackendAddressPool();
        backendAddressPool.setName("backendpool");
        ArrayList<BackendAddressPool> backendAddressPools = new ArrayList<>();
        backendAddressPools.add(backendAddressPool);
        loadBalancer.setBackendAddressPools(backendAddressPools);

        ResourceId poolId = new ResourceId();
        poolId.setId(getBackendPoolId(cloud, backendAddressPool.getName()));

        ResourceId configId = new ResourceId();
        configId.setId(getFrontendConfigId(cloud, frontendIpConfiguration.getName()));

        ArrayList<LoadBalancingRule> rules = new ArrayList<>();
        for (AzureLoadBalancerListenerConfig listener : getListeners()) {
            LoadBalancingRule rule = new LoadBalancingRule();
            rule.setName(listener.getProtocol() + "-" + listener.getSourcePort() + "-" + listener.getDestPort());
            rule.setBackendPort(listener.getDestPort());
            rule.setFrontendPort(listener.getSourcePort());
            rule.setProtocol(listener.getProtocol());
            rule.setIdleTimeoutInMinutes(getIdleTimeout());
            rule.setBackendAddressPool(poolId);
            rule.setFrontendIPConfiguration(configId);
            ResourceId probeId = new ResourceId();
            probeId.setId(getProbeId(cloud, "probe"));
            rule.setProbe(probeId);
            rules.add(rule);
        }

        loadBalancer.setLoadBalancingRules(rules);

        Probe probe = new Probe();
        probe.setName("probe");
        probe.setProtocol(getProbeProtocol());
        probe.setIntervalInSeconds(getProbeInterval());
        probe.setRequestPath(getProbePath());
        probe.setNumberOfProbes(getNumberOfProbes());
        probe.setPort(getProbePort());
        ArrayList<Probe> probes = new ArrayList<>();
        probes.add(probe);
        loadBalancer.setProbes(probes);

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        loadBalancer.setTags(tags);

        try {
            lBOperations.createOrUpdate(getResourceGroup(), getName(), loadBalancer);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update load balancer: " + getName());
        }

        try {
            for (LoadBalancer elb : lBOperations.list(getResourceGroup()).getLoadBalancers()) {
                if (elb.getName().equals(getName())) {
                    setPrivateIp(elb.getFrontendIpConfigurations().get(0).getPrivateIpAddress());
                }
            }
        } catch (Exception error) {
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, LoadBalancer> current, Set<String> changedProperties) {
        setPublicIpId(((LoadBalancerResource) current).getPublicIpId());
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        LoadBalancerOperations lBOperations = client.getLoadBalancersOperations();
        try {
            lBOperations.delete(getResourceGroup(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete load balancer: " + getName());
        }

        if (getPublicIpId() != null) {
            NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
            networkInterfaceResource.setRegion(getRegion());
            String[] idParts = getPublicIpId().split("/");
            String publicIpName = idParts[idParts.length-1];
            networkInterfaceResource.setName(publicIpName);
            networkInterfaceResource.releasePublicIp(cloud);
        }
    }

    @Override
    public String toString() {
        return toDisplayString();
    }

    @Override
    public String toDisplayString() {
        return "load balancer " + getName();
    }

    public String getBackendPoolId(AzureCloud cloud, String poolName) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/loadBalancers/", getName(),
                "/backendAddressPools/", poolName);
    }

    public String getProbeId(AzureCloud cloud, String probeName) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/loadBalancers/", getName(),
                "/probes/", probeName);
    }

    private String getFrontendConfigId(AzureCloud cloud, String configName) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/loadBalancers/", getName(),
                "/frontendIpConfigurations/", configName);
    }

    public String getIdFromName(AzureCloud cloud) {
        return String.format("%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/loadBalancers/", getName());
    }
}
