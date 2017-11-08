package beam.azure.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.network.ApplicationGatewayOperations;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.PublicIpAddressOperations;
import com.microsoft.azure.management.network.models.*;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.util.*;

public class ApplicationGatewayResource extends AzureResource<ApplicationGateway> {

    private String id;
    private String name;
    private List<AzureLoadBalancerListenerConfig> listenerConfigs;
    private String publicIp;
    private String publicIpId;
    private String tier;
    private Map<String, String> tags;
    private BeamReference loadBalancer;
    private BeamReference subnet;

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

    public List<AzureLoadBalancerListenerConfig> getListenerConfigs() {
        if (listenerConfigs == null) {
            listenerConfigs = new ArrayList<>();
        }

        return listenerConfigs;
    }

    public void setListenerConfigs(List<AzureLoadBalancerListenerConfig> listenerConfigs) {
        this.listenerConfigs = listenerConfigs;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
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

    @ResourceDiffProperty(updatable = true)
    public String getListenerHash() {
        String configHash = StringUtils.hex(StringUtils.md5(ObjectUtils.toJson(getListenerConfigs())));
        return configHash;
    }

    public BeamReference getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(BeamReference loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public BeamReference getSubnet() {
        return subnet;
    }

    public void setSubnet(BeamReference subnet) {
        this.subnet = subnet;
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
    public void init(AzureCloud cloud, BeamResourceFilter filter, ApplicationGateway applicationGateway) {
        setName(applicationGateway.getName());
        String publicIpId = applicationGateway.getFrontendIPConfigurations().get(0).getPublicIPAddress().getId();
        setPublicIpId(publicIpId);
        setPublicIp(getPublicIpFromId(cloud, publicIpId));
        setId(applicationGateway.getId());
        setTier(applicationGateway.getSku().getName());

        for (ApplicationGatewayRequestRoutingRule rule : applicationGateway.getRequestRoutingRules()) {
            AzureLoadBalancerListenerConfig config = new AzureLoadBalancerListenerConfig();

            for (ApplicationGatewayBackendHttpSettings backendHttpSettings : applicationGateway.getBackendHttpSettingsCollection()) {
                if (backendHttpSettings.getId().equals(rule.getBackendHttpSettings().getId())) {
                    config.setDestPort(backendHttpSettings.getPort());
                    config.setDestProtocol(backendHttpSettings.getProtocol());
                }
            }

            for (ApplicationGatewayHttpListener listener : applicationGateway.getHttpListeners()) {
                if (listener.getId().equals(rule.getHttpListener().getId())) {
                    for (ApplicationGatewayFrontendPort port : applicationGateway.getFrontendPorts()) {
                        if (port.getId().equals(listener.getFrontendPort().getId())) {
                            config.setSourcePort(port.getPort());
                        }
                    }

                    for (ApplicationGatewaySslCertificate sslCertificate : applicationGateway.getSslCertificates()) {
                        if (sslCertificate.getId().equals(listener.getSslCertificate().getId())) {
                            config.setSslCertificateName(sslCertificate.getName());
                        }
                    }

                    config.setProtocol(listener.getProtocol());
                }
            }

            getListenerConfigs().add(config);
        }

        if (!applicationGateway.getGatewayIPConfigurations().isEmpty()) {
            ResourceId subnetResourceId = applicationGateway.getGatewayIPConfigurations().get(0).getSubnet();
            if (subnetResourceId != null) {
                String subnetId = subnetResourceId.getId();
                setSubnet(newReference(SubnetResource.class, subnetId));
            }
        }

        setTags(applicationGateway.getTags());
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
        ApplicationGatewayOperations aGOperations = client.getApplicationGatewaysOperations();

        ApplicationGateway applicationGateway = new ApplicationGateway();
        applicationGateway.setName(getName());
        applicationGateway.setLocation(getRegion());

        ApplicationGatewaySku sku = new ApplicationGatewaySku();
        sku.setTier("Standard");
        sku.setCapacity(1);
        sku.setName(getTier());
        applicationGateway.setSku(sku);

        ArrayList<ApplicationGatewayIPConfiguration> gatewayIPConfigurations = new ArrayList<>();
        ApplicationGatewayIPConfiguration applicationGatewayIPConfiguration = new ApplicationGatewayIPConfiguration();
        gatewayIPConfigurations.add(applicationGatewayIPConfiguration);
        ResourceId subnetId = new ResourceId();
        SubnetResource subnetResource = (SubnetResource)getSubnet().resolve();
        subnetId.setId(subnetResource.getId());
        applicationGatewayIPConfiguration.setSubnet(subnetId);
        applicationGatewayIPConfiguration.setName("gatewayIpConfig");
        applicationGateway.setGatewayIPConfigurations(gatewayIPConfigurations);

        if (getPublicIpId() == null) {
            NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
            networkInterfaceResource.setRegion(getRegion());
            networkInterfaceResource.setName(getName());
            networkInterfaceResource.setPublicIpAllocation("Dynamic");
            String publicIpId = networkInterfaceResource.allocatePublicIp(cloud);
            setPublicIpId(publicIpId);
            setPublicIp(getPublicIpFromId(cloud, publicIpId));
        }

        ResourceId ipId = new ResourceId();
        ipId.setId(getPublicIpId());
        ApplicationGatewayFrontendIPConfiguration frontendIpConfiguration = new ApplicationGatewayFrontendIPConfiguration();
        frontendIpConfiguration.setName("frontendipconfig");
        frontendIpConfiguration.setPublicIPAddress(ipId);

        ArrayList<ApplicationGatewayFrontendIPConfiguration> frontendIpConfigurations = new ArrayList<>();
        frontendIpConfigurations.add(frontendIpConfiguration);
        applicationGateway.setFrontendIPConfigurations(frontendIpConfigurations);

        ApplicationGatewayBackendAddressPool backendAddressPool = new ApplicationGatewayBackendAddressPool();
        backendAddressPool.setName("backendpool");
        ArrayList<ApplicationGatewayBackendAddress> addresses = new ArrayList<>();
        ApplicationGatewayBackendAddress address = new ApplicationGatewayBackendAddress();
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource)getLoadBalancer().resolve();
        if (loadBalancerResource.getPrivateIp() != null) {
            address.setIpAddress(loadBalancerResource.getPrivateIp());
            addresses.add(address);
        }

        backendAddressPool.setBackendAddresses(addresses);

        ArrayList<ApplicationGatewayBackendAddressPool> backendAddressPools = new ArrayList<>();
        backendAddressPools.add(backendAddressPool);
        applicationGateway.setBackendAddressPools(backendAddressPools);

        ArrayList<ApplicationGatewayFrontendPort> frontendPorts = new ArrayList<>();
        ArrayList<ApplicationGatewayBackendHttpSettings> backendSettingsCollection = new ArrayList<>();
        ArrayList<ApplicationGatewayHttpListener> listeners = new ArrayList<>();
        ArrayList<ApplicationGatewayRequestRoutingRule> rules = new ArrayList<>();
        ArrayList<ApplicationGatewaySslCertificate> certificates = new ArrayList<>();

        for (AzureLoadBalancerListenerConfig listenerConfig : getListenerConfigs()) {
            ApplicationGatewayFrontendPort frontendPort = new ApplicationGatewayFrontendPort();
            frontendPort.setPort(listenerConfig.getSourcePort());
            frontendPort.setName("frontendPort-" + listenerConfig.getProtocol() + listenerConfig.getSourcePort() + listenerConfig.getDestProtocol() + listenerConfig.getDestPort());
            frontendPorts.add(frontendPort);

            ApplicationGatewayBackendHttpSettings backendHttpSettings = new ApplicationGatewayBackendHttpSettings();
            backendHttpSettings.setName("backendSetting-" + listenerConfig.getProtocol() + listenerConfig.getSourcePort() + listenerConfig.getDestProtocol() + listenerConfig.getDestPort());
            backendHttpSettings.setPort(listenerConfig.getDestPort());
            backendHttpSettings.setProtocol(listenerConfig.getDestProtocol());
            backendSettingsCollection.add(backendHttpSettings);

            ApplicationGatewayHttpListener listener = new ApplicationGatewayHttpListener();
            listener.setName("listener-" + listenerConfig.getProtocol() + listenerConfig.getSourcePort() + listenerConfig.getDestProtocol() + listenerConfig.getDestPort());
            ResourceId frontendIpconfigId = new ResourceId();
            ResourceId frontendPortId = new ResourceId();
            frontendIpconfigId.setId(getIdFromName(cloud, "frontendIPConfigurations", "frontendipconfig"));
            frontendPortId.setId(getIdFromName(cloud, "frontendPorts", frontendPort.getName()));
            listener.setFrontendPort(frontendPortId);
            listener.setFrontendIPConfiguration(frontendIpconfigId);
            listener.setProtocol(listenerConfig.getProtocol());

            if (listenerConfig.getSslCertificateName() != null) {
                ApplicationGatewaySslCertificate sslCertificate = new ApplicationGatewaySslCertificate();
                sslCertificate.setName(listenerConfig.getSslCertificateName());
                certificates.add(sslCertificate);

                ResourceId certificateId = new ResourceId();
                certificateId.setId(getIdFromName(cloud, "sslCertificates", sslCertificate.getName()));
                listener.setSslCertificate(certificateId);
            }

            listeners.add(listener);

            ApplicationGatewayRequestRoutingRule requestRoutingRule = new ApplicationGatewayRequestRoutingRule();
            requestRoutingRule.setName("rule-" + listenerConfig.getProtocol() + listenerConfig.getSourcePort() + listenerConfig.getDestProtocol() + listenerConfig.getDestPort());
            ResourceId listenerId = new ResourceId();
            ResourceId backPoolId = new ResourceId();
            ResourceId backendSettingsId = new ResourceId();
            listenerId.setId(getIdFromName(cloud, "httpListeners", listener.getName()));
            backPoolId.setId(getIdFromName(cloud, "backendAddressPools", "backendpool"));
            backendSettingsId.setId(getIdFromName(cloud, "backendHttpSettingsCollection", backendHttpSettings.getName()));
            requestRoutingRule.setHttpListener(listenerId);
            requestRoutingRule.setBackendAddressPool(backPoolId);
            requestRoutingRule.setBackendHttpSettings(backendSettingsId);
            rules.add(requestRoutingRule);
        }

        applicationGateway.setFrontendPorts(frontendPorts);
        applicationGateway.setBackendHttpSettingsCollection(backendSettingsCollection);
        applicationGateway.setSslCertificates(certificates);
        applicationGateway.setHttpListeners(listeners);
        applicationGateway.setRequestRoutingRules(rules);

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        applicationGateway.setTags(tags);

        try {
            aGOperations.beginCreateOrUpdating(getResourceGroup(), getName(), applicationGateway);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update application gateway: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, ApplicationGateway> current, Set<String> changedProperties) {
        setPublicIpId(((ApplicationGatewayResource) current).getPublicIpId());
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        ApplicationGatewayOperations aGOperations = client.getApplicationGatewaysOperations();
        try {
            aGOperations.beginDeleting(getResourceGroup(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete application gateway: " + getName());
        }
    }

    @Override
    public String toString() {
        return toDisplayString();
    }

    @Override
    public String toDisplayString() {
        return "application gateway " + getName();
    }

    public String getBackendPoolId(AzureCloud cloud, String poolName) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/loadBalancers/", getName(),
                "/backendAddressPools/", poolName);
    }

    private String getFrontendConfigId(AzureCloud cloud, String configName) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/applicationGateways/", getName(),
                "/applicationGatewayFrontendIPConfigurations/", configName);
    }

    public String getIdFromName(AzureCloud cloud, String className, String name) {
        return String.format("%s%s%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Network",
                "/applicationGateways/", getName(),
                "/" + className + "/", name);
    }
}
