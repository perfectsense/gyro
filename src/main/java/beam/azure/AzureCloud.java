package beam.azure;

import beam.*;
import beam.azure.config.*;
import beam.config.*;
import beam.diff.*;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.*;

import beam.enterprise.EnterpriseApi;
import beam.enterprise.EnterpriseException;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.ComputeManagementService;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.dns.DnsManagementClient;
import com.microsoft.azure.management.dns.DnsManagementService;
import com.microsoft.azure.management.network.NetworkResourceProviderService;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.resources.ResourceManagementService;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;

import javax.naming.ServiceUnavailableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.apache.http.message.BasicNameValuePair;

public class AzureCloud extends BeamCloud {

    private String account;
    private String project;

    private AzureCredentials credentials;

    /**
     * @param runtime Can't be {@code null}.
     */
    public AzureCloud(BeamRuntime runtime) {
        Preconditions.checkNotNull(runtime, "runtime");
        this.account = runtime.getAccount();
        this.project = runtime.getProject();
    }

    public AzureCloud(String project, String serial) {
        Preconditions.checkNotNull(project, "project");

        this.project = project;
        this.account = "default";
    }

    public DnsManagementClient createDnsManagementClient() {
        try {
            Configuration config = createConfiguration();
            return DnsManagementService.create(config);
        } catch (Exception error) {
            return null;
        }
    }

    public ComputeManagementClient createComputeManagementClient() {
        try {
            Configuration config = createConfiguration();
            return ComputeManagementService.create(config);
        } catch (Exception error) {
            return null;
        }
    }

    public NetworkResourceProviderClient createNetworkManagementClient() {
        try {
            Configuration config = createConfiguration();
            return NetworkResourceProviderService.create(config);
        } catch (Exception error) {
            return null;
        }
    }

    public ResourceManagementClient createResourceManagementClient() {
        try {
            Configuration config = createConfiguration();
            return ResourceManagementService.create(config);
        } catch (Exception error) {
            return null;
        }
    }

    public Configuration createConfiguration() throws Exception {
        String baseUri = "https://management.core.windows.net";
        return ManagementConfiguration.configure(
                null,
                new URI(baseUri),
                getCredentials().getSubscription(),
                getAccessTokenFromServicePrincipalCredentials().getAccessToken());
    }

    private AuthenticationResult getAccessTokenFromServicePrincipalCredentials() throws
            ServiceUnavailableException, MalformedURLException, ExecutionException, InterruptedException {
        AuthenticationContext context;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext("https://login.windows.net/" + getCredentials().getTenant(),
                    false, service);
            ClientCredential cred = new ClientCredential(getCredentials().getClientId(),
                    getCredentials().getClientKey());
            Future<AuthenticationResult> future = context.acquireToken(
                    "https://management.azure.com/", cred, null);
            result = future.get();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "authentication result was null");
        }

        return result;
    }

    @Override
    public String getName() {
        return "azure";
    }

    @Override
    public List<Diff<?, ?, ?>> findChanges(BeamRuntime runtime) throws Exception {
        RootConfig config = runtime.getConfig();

        if (config == null) {
            return null;
        }

        NetworkConfig networkConfig = config.getNetworkConfig();

        for (CloudConfig cloudConfig : networkConfig.getClouds()) {
            if (!(cloudConfig instanceof AzureCloudConfig)) {
                continue;
            }

            AzureResourceFilter filter = new AzureResourceFilter();
            filter.setIncludedLayers(getIncludedLayers());

            // Read the current state from Azure.
            AzureCloudConfig current = new AzureCloudConfig();
            AzureCloudConfig pending = (AzureCloudConfig) cloudConfig;

            String project = runtime.getProject();
            String serial = runtime.getSerial();
            String environment = runtime.getEnvironment();
            String internalDomain = runtime.getInternalDomain();
            String sandbox = String.valueOf(networkConfig.isSandbox());

            current.setActiveRegions(pending.getActiveRegions());
            current.setFilter(filter);
            current.init(this);

            // Hosted zones.
            String domain = networkConfig.getSubdomain();
            ZoneResource zoneResource = new ZoneResource();
            zoneResource.setName(domain);
            zoneResource.setRegion("global");
            pending.getZones().add(zoneResource);

            pending.setFilter(filter);
            Map<String, RecordSetResource> gatewayDnsMap = new HashMap<>();
            Map<String, RecordSetResource> gatewayPrivateDnsMap = new HashMap<>();
            Map<String, RecordSetResource> layerPrivateDnsMap = new HashMap<>();
            Map<String, RecordSetResource> loadBalancerDnsMap = new HashMap<>();

            Set<String> resourceGroupRegions = new HashSet<>();
            resourceGroupRegions.addAll(getActiveRegions());
            resourceGroupRegions.add("eastus");

            for (String region : resourceGroupRegions) {
                ResourceGroupResource resourceGroupResource = new ResourceGroupResource();
                String resourceGroupName = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), region);
                resourceGroupResource.setName(resourceGroupName);
                resourceGroupResource.setRegion(region);
                pending.getResourceGroups().add(resourceGroupResource);
            }

            // TODO: add logic for activeRegions
            for (AzureRegionConfig regionConfig : pending.getRegions()) {
                String region = regionConfig.getName();

                VirtualNetworkResource vNet = new VirtualNetworkResource();
                vNet.setRegion(region);
                vNet.setName(String.format("%s-v%s", project, serial));
                vNet.setCidrBlock(regionConfig.getCidr());
                vNet.getTags().put("Name", vNet.getName());

                // Key pair for the Vnet.
                KeyPairResource kpResource = new KeyPairResource();
                String keyName = String.format("%s-%s", project, region);
                kpResource.setRegion(region);
                kpResource.setName(keyName);
                vNet.getKeyPairs().add(kpResource);

                pending.getVns().add(vNet);

                Map<String, List<SubnetResource>> subnetResourcesByType = new HashMap<>();
                Set<String> publicSubnetTypes = new HashSet<>();

                for (SubnetConfig subnetConfig : regionConfig.getSubnets()) {
                    SubnetResource subnetResource = new SubnetResource();
                    subnetResource.setRegion(region);

                    List<String> types = new ArrayList<>();
                    types.addAll(subnetConfig.getTypes());
                    for (String type : types) {
                        List<SubnetResource> subnetResources = subnetResourcesByType.get(type);

                        if (subnetResources == null) {
                            subnetResources = new ArrayList<>();
                            subnetResourcesByType.put(type, subnetResources);
                        }

                        subnetResources.add(subnetResource);

                        if (subnetConfig.isPublicAccessible()) {
                            publicSubnetTypes.add(type);
                        }
                    }

                    subnetResource.setName(StringUtils.join(types, "-"));
                    subnetResource.setCidrBlock(subnetConfig.getCidr());
                    subnetResource.setVnet(subnetResource.newReference(vNet));

                    String ruleName = subnetResource.getName();
                    String sgName = String.format("%s-%s-v%s", project, ruleName, serial);
                    subnetResource.setSecurityGroup(sgName);

                    vNet.getSubnets().add(subnetResource);

                    if (subnetConfig.getGateway() != null) {
                        GatewayConfig gatewayConfig = subnetConfig.getGateway();
                        VirtualMachineResource virtualMachineResource = new VirtualMachineResource();
                        virtualMachineResource.setKeyPair(virtualMachineResource.newReference(kpResource));

                        virtualMachineResource.setName("az-" + UUID.randomUUID().toString().substring(0, 8));
                        virtualMachineResource.setRegion(region);
                        virtualMachineResource.setSubnet(virtualMachineResource.newReference(subnetResource));

                        for (VolumeConfig volumeConfig : gatewayConfig.getVolumes()) {
                            DiskResource diskResource = new DiskResource();
                            diskResource.setRegion(region);
                            diskResource.setName(volumeConfig.getName());
                            diskResource.setVirtualMachine(diskResource.newReference(virtualMachineResource));
                            diskResource.setSize(volumeConfig.getSize());
                            diskResource.setLun(volumeConfig.getLun());

                            virtualMachineResource.getDiskResources().add(diskResource);
                        }

                        virtualMachineResource.setImage(gatewayConfig.getImage());
                        virtualMachineResource.setInstanceType(gatewayConfig.getInstanceType());
                        virtualMachineResource.setState("VM running");

                        NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
                        networkInterfaceResource.setRegion(region);
                        networkInterfaceResource.setVirtualMachine(networkInterfaceResource.newReference(virtualMachineResource));
                        networkInterfaceResource.setName(virtualMachineResource.getName());
                        networkInterfaceResource.setPrivateIp(gatewayConfig.getIpAddress());
                        networkInterfaceResource.setDynamic(gatewayConfig.getIpAddress() == null);
                        virtualMachineResource.getNetworkInterfaceResources().add(networkInterfaceResource);

                        if (gatewayConfig.getElasticIp()) {
                            networkInterfaceResource.setPublicIpAllocation("Static");
                        } else {
                            networkInterfaceResource.setPublicIpAllocation("Dynamic");
                        }

                        if (gatewayConfig.getElasticIp() && gatewayConfig.getPublicIpAddress() != null) {
                            networkInterfaceResource.setPublicIp(gatewayConfig.getPublicIpAddress());
                        }

                        virtualMachineResource.setUserData(BaseEncoding.base64().encode(ObjectUtils.toJson(new ImmutableMap.Builder<>().
                                put("project", project).
                                put("environment", environment).
                                put("serial", serial).
                                put("internalDomain", internalDomain).
                                put("sandbox", sandbox).
                                put("layer", "gateway").
                                put("region", region).
                                build()).getBytes(com.psddev.dari.util.StringUtils.UTF_8)));

                        virtualMachineResource.setBeamLaunchIndex(0);
                        virtualMachineResource.getTags().put("beam.env", "network");
                        virtualMachineResource.getTags().put("beam.layer", "gateway");

                        // DNS
                        if (gatewayConfig.getHostnames().isEmpty()) {
                            gatewayConfig.getHostnames().add("vpn");
                        }

                        for (String hostname : gatewayConfig.getHostnames()) {
                            if (hostname.lastIndexOf(domain) != -1) {
                                hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                            }

                            RecordSetResource record = gatewayDnsMap.get(hostname);
                            if (record == null) {
                                record = new RecordSetResource();
                                record.getTags().put("beam.env", "network");
                                record.getTags().put("beam.layer", "gateway");
                                record.setName(hostname);
                                record.setRegion("global");
                                record.setType("A");
                                record.setTTL(60);
                                record.setZone(record.newReference(zoneResource));
                                zoneResource.getRecords().add(record);

                                gatewayDnsMap.put(hostname, record);
                            }

                            record.getValues().add(new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource)));
                        }

                        for (String hostname : gatewayConfig.getPrivateHostnames()) {
                            if (hostname.lastIndexOf(domain) != -1) {
                                hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                            }

                            RecordSetResource record = gatewayPrivateDnsMap.get(hostname);
                            if (record == null) {
                                record = new RecordSetResource();
                                record.getTags().put("beam.env", "network");
                                record.getTags().put("beam.layer", "gateway");
                                record.setName(hostname);
                                record.setRegion("global");
                                record.setType("A");
                                record.setTTL(60);
                                record.setZone(record.newReference(zoneResource));
                                zoneResource.getRecords().add(record);

                                gatewayPrivateDnsMap.put(hostname, record);
                            }

                            RecordSetResource.ReferenceValue referenceValue = new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource));
                            referenceValue.setPrivate(true);
                            record.getValues().add(referenceValue);
                        }

                        subnetResource.getVirtualMachines().add(virtualMachineResource);
                    }
                }

                Map<String, LoadBalancerResource> loadBalancers = new HashMap<>();

                for (AzureLoadBalancerConfig loadBalancerConfig : regionConfig.getLoadBalancers()) {
                    AvailabilitySetResource availabilitySetResource = new AvailabilitySetResource();
                    availabilitySetResource.setRegion(region);
                    availabilitySetResource.setName(loadBalancerConfig.getName());
                    availabilitySetResource.setFaultDomainCount(3);
                    availabilitySetResource.setUpdateDomainCount(5);

                    availabilitySetResource.getTags().put("beam.env", "network");
                    vNet.getAvailabilitySets().add(availabilitySetResource);

                    LoadBalancerResource loadBalancerResource = new LoadBalancerResource();
                    loadBalancerResource.setRegion(region);
                    loadBalancerResource.setName(loadBalancerConfig.getName());

                    ApplicationGatewayResource applicationGatewayResource = new ApplicationGatewayResource();
                    if (loadBalancerConfig.getTier() != null) {
                        applicationGatewayResource.setRegion(region);
                        applicationGatewayResource.setName(loadBalancerConfig.getName());

                        for (AzureLoadBalancerListenerConfig listenerConfig : loadBalancerConfig.getListeners()) {
                            listenerConfig.getDestProtocol();
                        }

                        applicationGatewayResource.setListenerConfigs(loadBalancerConfig.getListeners());
                        applicationGatewayResource.getTags().put("beam.env", "network");
                        applicationGatewayResource.setTier(loadBalancerConfig.getTier());
                        applicationGatewayResource.setLoadBalancer(applicationGatewayResource.newReference(loadBalancerResource));

                        List<SubnetResource> subnets = subnetResourcesByType.get(loadBalancerConfig.getSubnetType());
                        if (!ObjectUtils.isBlank(subnets)) {
                            applicationGatewayResource.setSubnet(applicationGatewayResource.newReference(subnets.get(0)));
                        }

                        vNet.getApplicationGateways().add(applicationGatewayResource);
                        loadBalancerResource.setListeners(loadBalancerConfig.getListeners());

                    } else {
                        loadBalancerResource.setListeners(loadBalancerConfig.getListeners());
                    }

                    loadBalancerResource.setIdleTimeout(loadBalancerConfig.getIdleTimeout());
                    loadBalancerResource.setHostnames(loadBalancerConfig.getHostnames());
                    loadBalancerResource.setVerificationHostnames(loadBalancerConfig.getVerificationHostnames());

                    AzureLoadBalancerProbeConfig probeConfig = loadBalancerConfig.getProbe();
                    loadBalancerResource.setProbePort(probeConfig.getPort());
                    loadBalancerResource.setProbeInterval(probeConfig.getInterval());
                    loadBalancerResource.setProbePath(probeConfig.getPath());
                    loadBalancerResource.setProbeProtocol(probeConfig.getProtocol());
                    loadBalancerResource.setNumberOfProbes(probeConfig.getNumberOfProbes());

                    loadBalancerResource.setAvailabilitySet(loadBalancerResource.newReference(availabilitySetResource));

                    List<SubnetResource> subnets = subnetResourcesByType.get(loadBalancerConfig.getInternalSubnet());
                    if (!ObjectUtils.isBlank(subnets) && loadBalancerConfig.getTier() != null) {
                        loadBalancerResource.setSubnet(loadBalancerResource.newReference(subnets.get(0)));
                    }

                    loadBalancerResource.getTags().put("beam.env", "network");
                    vNet.getLoadBalancers().add(loadBalancerResource);
                    loadBalancers.put(loadBalancerResource.getName(), loadBalancerResource);

                    for (String hostname : loadBalancerResource.getHostnames()) {
                        if (hostname.lastIndexOf(domain) != -1) {
                            hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                        }

                        RecordSetResource record = loadBalancerDnsMap.get(hostname);
                        if (record == null) {
                            record = new RecordSetResource();
                            record.getTags().put("beam.env", "network");
                            record.getTags().put("beam.layer", "loadBalancer");
                            record.setName(hostname);
                            record.setRegion("global");
                            record.setType("A");
                            record.setTTL(60);
                            record.setZone(record.newReference(zoneResource));
                            zoneResource.getRecords().add(record);

                            loadBalancerDnsMap.put(hostname, record);
                        }

                        if (loadBalancerConfig.getTier() != null) {
                            record.getValues().add(new RecordSetResource.ReferenceValue(record.newReference(applicationGatewayResource)));
                        } else {
                            record.getValues().add(new RecordSetResource.ReferenceValue(record.newReference(loadBalancerResource)));
                        }
                    }
                }

                LAYERS: for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    String layerName = layer.getName();

                    DeploymentConfig deployment = layer.getDeployment();
                    String buildPath = null;
                    String buildNumber = null;
                    Map<String, String> dataMap = new HashMap<>();

                    if (deployment != null) {
                        dataMap = deployment.prepare(this, pending);
                    }

                    Map<String, String> baseMetadata = new HashMap<>();
                    baseMetadata.put("project", project);
                    baseMetadata.put("environment", environment);
                    baseMetadata.put("serial", serial);
                    baseMetadata.put("internalDomain", internalDomain);
                    baseMetadata.put("sandbox", sandbox);
                    baseMetadata.put("build", "prod");
                    baseMetadata.put("region", region);

                    if (deployment != null) {
                        baseMetadata.putAll(deployment.getGroupHashItems());
                    }

                    Map userDataMap = new ImmutableMap.Builder<>().
                            putAll(baseMetadata).
                            put("layer", layerName).
                            putAll(dataMap).build();

                    String userData = BaseEncoding.base64().encode(ObjectUtils.toJson(userDataMap).getBytes(com.psddev.dari.util.StringUtils.UTF_8));

                    String userDataAutoScale = BaseEncoding.base64().encode(ObjectUtils.toJson(new ImmutableMap.Builder<>().
                            putAll(userDataMap).
                            put("autoscale", "true").
                            build()).getBytes(com.psddev.dari.util.StringUtils.UTF_8));

                    for (PlacementConfig placement : layer.getPlacements()) {
                        String subnetType = placement.getSubnetType();
                        List<SubnetResource> subnetResources = subnetResourcesByType.get(subnetType);
                        List<String> elasticIpList = new ArrayList<>();

                        if (placement.getElasticIp() && !placement.getElasticIps().isEmpty()) {
                            elasticIpList.addAll(placement.getElasticIps());
                        }

                        if (subnetResources == null) {
                            continue LAYERS;
                        }

                        AutoScaleConfig as = placement.getAutoscale();
                        int subnetResourcesSize = subnetResources.size();

                        // Static number of instances.
                        if (as == null) {
                            int placementSizePerSubnet = placement.getSizePerSubnet();

                            // Configure hostnames.
                            List<RecordSetResource> hostnameResources = new ArrayList<>();
                            if (placement.getHostnames().size() > 0) {
                                for (String hostname : placement.getHostnames()) {
                                    if (hostname.lastIndexOf(domain) != -1) {
                                        hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                                    }

                                    RecordSetResource record = new RecordSetResource();

                                    zoneResource.getRecords().add(record);
                                    record.getTags().put("beam.env", runtime.getEnvironment());
                                    record.getTags().put("beam.layer", layerName);
                                    record.setName(hostname);
                                    record.setTTL(60);
                                    record.setType("A");
                                    record.setZone(record.newReference(zoneResource));
                                    record.setRegion("global");

                                    hostnameResources.add(record);
                                }
                            }

                            List<RecordSetResource> privateHostnameResources = new ArrayList<>();
                            for (String hostname : placement.getPrivateHostnames()) {
                                if (hostname.lastIndexOf(domain) != -1) {
                                    hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                                }

                                RecordSetResource record = layerPrivateDnsMap.get(hostname);
                                if (record == null) {
                                    record = new RecordSetResource();
                                    record.getTags().put("beam.env", runtime.getEnvironment());
                                    record.getTags().put("beam.layer", layerName);
                                    record.setTTL(60);
                                    record.setType("A");
                                    record.setName(hostname);
                                    zoneResource.getRecords().add(record);
                                    record.setZone(record.newReference(zoneResource));
                                    record.setRegion("global");

                                    layerPrivateDnsMap.put(hostname, record);
                                    privateHostnameResources.add(record);
                                }
                            }

                            int subnetIndex = 0;
                            for (SubnetResource subnetResource : subnetResources) {
                                Integer beamLaunchIndex = 0;

                                for (int i = 0; i < placementSizePerSubnet; i++) {
                                    VirtualMachineResource virtualMachineResource = new VirtualMachineResource();
                                    virtualMachineResource.setKeyPair(virtualMachineResource.newReference(kpResource));

                                    virtualMachineResource.setName("az-" + UUID.randomUUID().toString().substring(0, 8));
                                    virtualMachineResource.setRegion(region);
                                    virtualMachineResource.setSubnet(virtualMachineResource.newReference(subnetResource));

                                    for (VolumeConfig volumeConfig : layer.getVolumes()) {
                                        DiskResource diskResource = new DiskResource();
                                        diskResource.setRegion(region);
                                        diskResource.setName(volumeConfig.getName());
                                        diskResource.setVirtualMachine(diskResource.newReference(virtualMachineResource));
                                        diskResource.setSize(volumeConfig.getSize());
                                        diskResource.setLun(volumeConfig.getLun());

                                        virtualMachineResource.getDiskResources().add(diskResource);
                                    }

                                    virtualMachineResource.setImage(layer.getImage());
                                    virtualMachineResource.setInstanceType(layer.getInstanceType());
                                    virtualMachineResource.setState("VM running");

                                    NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
                                    networkInterfaceResource.setRegion(region);
                                    networkInterfaceResource.setVirtualMachine(networkInterfaceResource.newReference(virtualMachineResource));
                                    networkInterfaceResource.setName(virtualMachineResource.getName());
                                    networkInterfaceResource.setDynamic(true);
                                    virtualMachineResource.getNetworkInterfaceResources().add(networkInterfaceResource);

                                    if (placement.getElasticIp()) {
                                        networkInterfaceResource.setPublicIpAllocation("Static");
                                    } else if (publicSubnetTypes.contains(subnetType)) {
                                        networkInterfaceResource.setPublicIpAllocation("Dynamic");
                                    } else {
                                        networkInterfaceResource.setPublicIpAllocation("None");
                                    }

                                    int elasticIpIndex = subnetIndex * placementSizePerSubnet + i;
                                    if (placement.getElasticIp() && elasticIpIndex < elasticIpList.size()) {
                                        networkInterfaceResource.setPublicIp(elasticIpList.get(elasticIpIndex));
                                    }

                                    virtualMachineResource.setBeamLaunchIndex(beamLaunchIndex++);
                                    virtualMachineResource.getTags().put("beam.env", environment);
                                    virtualMachineResource.getTags().put("beam.layer", layerName);

                                    subnetResource.getVirtualMachines().add(virtualMachineResource);

                                    if (virtualMachineResource.getUserData() == null) {
                                        virtualMachineResource.setUserData(userData);
                                    }

                                    for (RecordSetResource record : hostnameResources) {

                                        RecordSetResource.ReferenceValue value;
                                        if (publicSubnetTypes.contains(subnetType)) {
                                            value = new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource));

                                        } else {
                                            value = new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource));
                                            value.setPrivate(true);
                                        }

                                        value.setType("A");
                                        record.getValues().add(value);
                                    }

                                    for (RecordSetResource record : privateHostnameResources) {
                                        RecordSetResource.ReferenceValue value;
                                        value = new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource));
                                        value.setPrivate(true);
                                        value.setType("A");
                                        record.getValues().add(value);
                                    }
                                }

                                subnetIndex++;
                            }

                        } else {
                            int placementSizePerSubnet = as.getMaxPerSubnet();
                            int subnetIndex = 0;
                            Set<VirtualMachineResource> virtualMachineResources = new HashSet<>();
                            String groupName;
                            AzureGroupResource groupResource = new AzureGroupResource();
                            groupResource.setRegion(region);

                            if (placementSizePerSubnet > 0) {
                                vNet.getAzureGroupResources().add(groupResource);
                            }

                            for (String elbName : as.getLoadBalancers()) {
                                LoadBalancerResource loadBalancerResource = loadBalancers.get(elbName);
                                groupResource.getLoadBalancers().add(groupResource.newReference(loadBalancerResource));
                            }

                            if (deployment == null) {
                                groupName = String.format("%s %s %s v%s %s", project, layerName, environment, serial, region);

                            } else {
                                Map<String, String> groupHashItems = deployment.getGroupHashItems();

                                if (groupHashItems.containsKey("buildNumber")) {
                                    buildNumber = groupHashItems.get("buildNumber");
                                } else {
                                    buildNumber = "";
                                }

                                if (groupHashItems.containsKey("jenkinsBuildPath")) {
                                    buildPath = groupHashItems.get("jenkinsBuildPath");
                                } else {
                                    buildPath = "";
                                }

                                groupName = String.format("%s %s %s v%s %s %s %s", project, layerName, environment, serial, buildPath, buildNumber, region);
                            }

                            StringBuilder groupHashBuilder = new StringBuilder();
                            appendHash(groupHashBuilder, "image", layer.getImage());

                            if (deployment != null) {
                                Map<String, String> groupHashItems = deployment.getGroupHashItems();
                                for (String key : groupHashItems.keySet()) {
                                    String value = groupHashItems.get(key);
                                    appendHash(groupHashBuilder, key, value);
                                }

                                groupResource.setDeployment(deployment);
                            }

                            String groupHash = com.psddev.dari.util.StringUtils.hex(com.psddev.dari.util.StringUtils.md5(groupHashBuilder.toString()));
                            groupResource.setGroupHash(groupHash);
                            groupName = groupName + " " + groupHash;
                            groupResource.setName(groupName);

                            for (SubnetResource subnetResource : subnetResources) {
                                Integer beamLaunchIndex = 0;

                                for (int i = 0; i < placementSizePerSubnet; i++) {
                                    VirtualMachineResource virtualMachineResource = new VirtualMachineResource();
                                    virtualMachineResource.setKeyPair(virtualMachineResource.newReference(kpResource));
                                    virtualMachineResources.add(virtualMachineResource);

                                    virtualMachineResource.setName("az-" + UUID.randomUUID().toString().substring(0, 8));
                                    virtualMachineResource.setRegion(region);
                                    virtualMachineResource.setSubnet(virtualMachineResource.newReference(subnetResource));

                                    for (VolumeConfig volumeConfig : layer.getVolumes()) {
                                        DiskResource diskResource = new DiskResource();
                                        diskResource.setRegion(region);
                                        diskResource.setName(volumeConfig.getName());
                                        diskResource.setVirtualMachine(diskResource.newReference(virtualMachineResource));
                                        diskResource.setSize(volumeConfig.getSize());
                                        diskResource.setLun(volumeConfig.getLun());

                                        virtualMachineResource.getDiskResources().add(diskResource);
                                    }

                                    virtualMachineResource.setImage(layer.getImage());
                                    virtualMachineResource.setInstanceType(layer.getInstanceType());
                                    virtualMachineResource.setState("VM running");

                                    NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
                                    networkInterfaceResource.setRegion(region);
                                    networkInterfaceResource.setVirtualMachine(networkInterfaceResource.newReference(virtualMachineResource));
                                    networkInterfaceResource.setName(virtualMachineResource.getName());
                                    networkInterfaceResource.setDynamic(true);
                                    virtualMachineResource.getNetworkInterfaceResources().add(networkInterfaceResource);

                                    if (placement.getElasticIp()) {
                                        networkInterfaceResource.setPublicIpAllocation("Static");
                                    } else if (publicSubnetTypes.contains(subnetType)) {
                                        networkInterfaceResource.setPublicIpAllocation("Dynamic");
                                    } else {
                                        networkInterfaceResource.setPublicIpAllocation("None");
                                    }

                                    int elasticIpIndex = subnetIndex * placementSizePerSubnet + i;
                                    if (placement.getElasticIp() && elasticIpIndex < elasticIpList.size()) {
                                        networkInterfaceResource.setPublicIp(elasticIpList.get(elasticIpIndex));
                                    }

                                    virtualMachineResource.setBeamLaunchIndex(beamLaunchIndex++);
                                    virtualMachineResource.getTags().put("beam.env", environment);
                                    virtualMachineResource.getTags().put("beam.layer", layerName);
                                    virtualMachineResource.getTags().put("group", groupName);

                                    virtualMachineResource.setId(virtualMachineResource.getIdFromName(this));

                                    for (BeamReference loadBalancerResource : groupResource.getLoadBalancers()) {
                                        AvailabilitySetResource availabilitySetResource = new AvailabilitySetResource();
                                        availabilitySetResource.setRegion(region);
                                        availabilitySetResource.setName(loadBalancerResource.awsId());

                                        virtualMachineResource.setAvailabilitySet(availabilitySetResource.getIdFromName(this));
                                    }

                                    if (virtualMachineResource.getUserData() == null) {
                                        virtualMachineResource.setUserData(userDataAutoScale);
                                    }

                                    groupResource.setImage(virtualMachineResource.getImage());
                                    groupResource.setInstanceType(virtualMachineResource.getInstanceType());
                                    groupResource.setTags(virtualMachineResource.getTags());
                                    groupResource.getVirtualMachines().add(virtualMachineResource);
                                }

                                groupResource.setSize(groupResource.getVirtualMachines().size());
                                subnetIndex++;
                            }
                        }
                    }
                }

                for (SecurityRuleConfig rule : networkConfig.getRules()) {
                    SecurityGroupResource sgResource = new SecurityGroupResource();
                    sgResource.setRegion(region);

                    String ruleName = rule.getName();
                    String sgName = String.format("%s-%s-v%s", project, ruleName, serial);

                    vNet.getSecurityGroups().add(sgResource);
                    sgResource.setBeamId("sg-" + ruleName);
                    sgResource.setGroupName(sgName);
                    sgResource.setName(sgResource.getGroupName());
                    sgResource.getTags().put("Name", ruleName);

                    vNet.getSecurityGroups().add(sgResource);

                    int priority = 100;
                    for (AccessPermissionConfig perm : rule.getPermissions()) {
                        String cidr = perm.getCidr();
                        if (cidr == null) {
                            continue;
                        }

                        if (!Character.isDigit(cidr.charAt(0))) {
                            perm.setName(cidr);
                            if (subnetResourcesByType.get(cidr) != null) {
                                SubnetResource subnetResource = subnetResourcesByType.get(cidr).get(0);
                                cidr = subnetResource.getCidrBlock();
                            } else {
                                continue;
                            }
                        }

                        if ("0.0.0.0/0".equals(cidr)) {
                            cidr = "*";
                        }

                        List<Integer> ports = perm.getPorts();

                        // If no ports are specified add port "-1" to allow all traffic.
                        if (ObjectUtils.isBlank(ports)) {
                            ports.add(-1);
                        }

                        if (perm.getName() == null) {
                            perm.setName("p" + priority);
                        }

                        int priorityDelta = 0;
                        for (Integer port : ports) {
                            SecurityRuleResource ruleResource = new SecurityRuleResource();
                            ruleResource.setRegion(sgResource.getRegion());

                            String portRange;

                            if (port == -1) {
                                portRange = "*";

                            } else {
                                portRange = port.toString();
                            }

                            ruleResource.setPortRange(portRange);

                            if ("tcp".equals(perm.getProtocol())) {
                                ruleResource.setIpProtocol(null);
                            } else {
                                ruleResource.setIpProtocol(perm.getProtocol());
                            }

                            ruleResource.setIpRange(cidr);
                            ruleResource.setName(perm.getName() + "-" + ("*".equals(portRange) ? "all" : portRange));
                            ruleResource.setAccess("Allow");
                            ruleResource.setFromGroup(ruleResource.newReference(sgResource));
                            ruleResource.setPriority(priority + priorityDelta);
                            priorityDelta += 1;

                            sgResource.getRuleResources().add(ruleResource);
                        }

                        priority += 100;
                    }

                    SecurityRuleResource selfRule = new SecurityRuleResource();
                    selfRule.setRegion(sgResource.getRegion());
                    String selfCidr = ruleName;

                    if (subnetResourcesByType.get(selfCidr) != null) {
                        SubnetResource subnetResource = subnetResourcesByType.get(selfCidr).get(0);
                        selfCidr = subnetResource.getCidrBlock();
                        selfRule.setPortRange("*");
                        selfRule.setIpRange(selfCidr);
                        selfRule.setName("self");
                        selfRule.setAccess("Allow");
                        selfRule.setFromGroup(selfRule.newReference(sgResource));
                        selfRule.setPriority(3000);
                        sgResource.getRuleResources().add(selfRule);
                    }

                    SecurityRuleResource ruleResource = new SecurityRuleResource();
                    ruleResource.setRegion(sgResource.getRegion());

                    String cidr = "*";
                    String portRange;
                    portRange = "*";

                    ruleResource.setPortRange(portRange);
                    ruleResource.setIpRange(cidr);
                    ruleResource.setName("default");
                    ruleResource.setAccess("Deny");
                    ruleResource.setFromGroup(ruleResource.newReference(sgResource));
                    ruleResource.setPriority(4000);

                    sgResource.getRuleResources().add(ruleResource);
                }
            }

            BeamResource.updateTree(current);
            BeamResource.updateTree(pending);

            List<Diff<?, ?, ?>> diffs = new ArrayList<>();

            ResourceDiff resourceGroupDiff = new ResourceDiff(
                    this,
                    null,
                    ResourceGroupResource.class,
                    current.getResourceGroups(),
                    pending.getResourceGroups());

            resourceGroupDiff.diff();
            resourceGroupDiff.getChanges().clear();
            resourceGroupDiff.diff();
            diffs.add(resourceGroupDiff);

            ResourceDiff corsRulesDiff = new ResourceDiff(
                    this,
                    null,
                    AzureCorsRuleResource.class,
                    current.getCorsRules(),
                    pending.getCorsRules());

            corsRulesDiff.diff();
            corsRulesDiff.getChanges().clear();
            corsRulesDiff.diff();
            diffs.add(corsRulesDiff);

            ResourceDiff containerDiff = new ResourceDiff(
                    this,
                    null,
                    BlobContainerResource.class,
                    current.getBlobContainers(),
                    pending.getBlobContainers());

            containerDiff.diff();
            containerDiff.getChanges().clear();
            containerDiff.diff();
            diffs.add(containerDiff);

            ResourceDiff virtualNetworkDiff = new ResourceDiff(
                    this,
                    null,
                    VirtualNetworkResource.class,
                    current.getVns(),
                    pending.getVns());

            virtualNetworkDiff.diff();
            virtualNetworkDiff.getChanges().clear();
            virtualNetworkDiff.diff();
            diffs.add(virtualNetworkDiff);

            ResourceDiff zoneDiff = new ResourceDiff(
                    this,
                    null,
                    ZoneResource.class,
                    current.getZones(),
                    pending.getZones());

            zoneDiff.diff();
            zoneDiff.getChanges().clear();
            zoneDiff.diff();
            diffs.add(zoneDiff);

            // get changes in auto scaling groups
            List<Change<?>> changes = new ArrayList<>();
            findChangesByResourceClass(diffs, AzureGroupResource.class, changes);
            findChangesByType(changes, ChangeType.CREATE);

            List<AzureGroupResource> groupResources = new ArrayList<>();
            List<LoadBalancerResource> loadBalancerResources = new ArrayList<>();

            for (VirtualNetworkResource virtualNetworkResource : current.getVns()) {
                for (AzureGroupResource groupResource : virtualNetworkResource.getAzureGroupResources()) {
                    groupResources.add(groupResource);
                }

                for (LoadBalancerResource loadBalancerResource : virtualNetworkResource.getLoadBalancers()) {
                    loadBalancerResources.add(loadBalancerResource);
                }
            }

            for (VirtualNetworkResource virtualNetworkResource : pending.getVns()) {
                for (LoadBalancerResource loadBalancerResource : virtualNetworkResource.getLoadBalancers()) {
                    if (loadBalancerResource.getHostnames() != null && loadBalancerResource.getVerificationHostnames() != null) {
                        for (LoadBalancerResource elb : loadBalancerResources) {
                            if (elb.getName().equals(loadBalancerResource.getName()) && elb.getRegion().equals(loadBalancerResource.getRegion())) {
                                elb.getHostnames().addAll(loadBalancerResource.getHostnames());
                                elb.getVerificationHostnames().addAll(loadBalancerResource.getVerificationHostnames());
                            }
                        }
                    }
                }
            }

            prepareDeploymentResources(changes, current, pending, groupResources, loadBalancerResources);

            if (!"network".equals(runtime.getEnvironment())) {
                ResourceDiff deploymentDiffs = new ResourceDiff(
                        this,
                        pending.getFilter(),
                        DeploymentResource.class,
                        current.getDeployments(),
                        pending.getDeployments());

                deploymentDiffs.diff();
                deploymentDiffs.getChanges().clear();
                deploymentDiffs.diff();
                diffs.add(deploymentDiffs);
            }

            tryToKeep(diffs);
            return diffs;
        }

        return new ArrayList<>();
    }

    private void tryToKeep(List<Diff<?, ?, ?>> diffs) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                if (change instanceof ResourceUpdate) {
                    ((ResourceUpdate) change).tryToKeep();
                }

                tryToKeep(change.getDiffs());
            }
        }
    }

    private void findChangesByResourceClass(List<Diff<?, ?, ?>> diffs, Class<?> resourceClass, List<Change<?>> changes) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                BeamResource resource = ((ResourceChange)change).getResource();
                if (resource.getClass().equals(resourceClass)) {
                    changes.add(change);
                } else {
                    findChangesByResourceClass(change.getDiffs(), resourceClass, changes);
                }
            }
        }
    }

    private void findChangesByType(List<Change<?>> changes, ChangeType type) {
        Iterator<Change<?>> iter = changes.iterator();
        while (iter.hasNext()) {
            Change<?> change = iter.next();
            if (change.getType() != type) {
                iter.remove();
            }
        }
    }

    private void prepareDeploymentResources(List<Change<?>> changes, AzureCloudConfig current, AzureCloudConfig pending, List<AzureGroupResource> groupResources, List<LoadBalancerResource> loadBalancerResources) {
        Map<String, DeploymentResource> deploymentResources = new LinkedHashMap<>();
        for (DeploymentResource currentDeployment : current.getDeployments()) {
            deploymentResources.put(currentDeployment.getGroupHash(), currentDeployment);
        }

        current.getDeployments().clear();

        for(Change<?> change : changes) {
            BeamResource resource = ((ResourceChange)change).getResource();
            AzureGroupResource asgResource = (AzureGroupResource)resource;

            DeploymentResource deploymentResource = deploymentResources.get(asgResource.getGroupHash());
            if (deploymentResource == null) {
                deploymentResource = new DeploymentResource();
                deploymentResource.setGroupHash(asgResource.getGroupHash());

                DeploymentConfig deployment = asgResource.getDeployment();

                deploymentResource.setImage(asgResource.getImage());
                deploymentResource.setInstanceType(asgResource.getInstanceType());

                if (deployment != null) {
                    deploymentResource.setDeploymentString(deployment.toDisplayString());
                }

                deploymentResources.put(asgResource.getGroupHash(), deploymentResource);
            }

            deploymentResource.getAutoscaleGroups().add(asgResource);
        }

        for (DeploymentResource deploymentResource : deploymentResources.values()) {
            deploymentResource.setCurrentGroups(groupResources);
            deploymentResource.setCurrentElbs(loadBalancerResources);

            ZoneResource zoneResource = null;
            for (ZoneResource zone : pending.getZones()) {
                if (zone.getName().equals(BeamRuntime.getCurrentRuntime().getSubDomain())) {
                    zoneResource = zone;
                }
            }

            deploymentResource.setZoneResource(zoneResource);

            pending.getDeployments().add(deploymentResource);
            current.getDeployments().add(deploymentResource);
        }
    }

    @Override
    public List<? extends BeamInstance> getInstances(boolean cacheOk) throws Exception {
        List<AzureInstance> instances = new ArrayList<>();
        ComputeManagementClient computeManagementClient = this.createComputeManagementClient();
        VirtualMachineOperations vMOperations = computeManagementClient.getVirtualMachinesOperations();

        for (String region : this.getActiveRegions()) {
            String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), region);

            try {
                for (VirtualMachine vm : vMOperations.list(resourceGroup).getVirtualMachines()) {
                    VirtualMachine virtualMachine = vMOperations.getWithInstanceView(resourceGroup, vm.getName()).getVirtualMachine();
                    AzureInstance instance = new AzureInstance(this, virtualMachine);
                    instances.add(instance);
                }

            } catch (Exception error) {
                error.printStackTrace();
                throw new BeamException("Fail to get azure instances!");
            }
        }

        return instances;
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh) {
        if (getCredentials() == null) {
            return null;
        }

        return getCredentials().toMap();
    }

    public AzureCredentials getCredentials() {
        if (credentials != null) {
            return credentials;
        }

        File credentialsPath = new File(System.getProperty("user.home") +  File.separator + ".beam" + File.separator + "azure.yml");
        if (!credentialsPath.exists()) {
            credentialsPath = new File("/etc/beam/azure.yml");
        }

        if (credentialsPath.exists()) {
            Yaml yaml = new Yaml();
            try {
                Map map = (Map) yaml.load(new FileInputStream(credentialsPath));
                if (map != null) {
                    List accounts = (List) map.get("accounts");
                    if (accounts != null) {
                        for (Object m : accounts) {
                            Map accountMap = (Map) m;
                            if (account.equals((String) accountMap.get("name"))) {
                                List projects = (List) accountMap.get("projects");
                                for (Object n : projects) {
                                    Map projectMap = (Map) n;
                                    if (project.equals((String) projectMap.get("name"))) {
                                        credentials = new AzureCredentials(
                                                (String) accountMap.get("subscription"),
                                                (String) projectMap.get("clientId"),
                                                (String) projectMap.get("clientKey"),
                                                (String) projectMap.get("tenant"),
                                                (String) projectMap.get("storageName"),
                                                (String) projectMap.get("storageKey"));

                                        return credentials;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch(Exception ex) {
                credentials = null;
            }
        }

        if (credentials == null && EnterpriseApi.isAvailable()) {
            try {
                Map<String, Object> exchangeMap = EnterpriseApi.call(
                        "azure/exchange-session",
                        new BasicNameValuePair("accountName", account),
                        new BasicNameValuePair("projectName", project));

                switch (ObjectUtils.to(String.class, exchangeMap.get("status"))) {
                    case "ok":
                        @SuppressWarnings("unchecked")
                        Map<String, Object> credentialsMap = (Map<String, Object>) exchangeMap.get("credentials");

                        credentials = new AzureCredentials(
                                ObjectUtils.to(String.class, credentialsMap.get("subscription")),
                                ObjectUtils.to(String.class, credentialsMap.get("clientId")),
                                ObjectUtils.to(String.class, credentialsMap.get("clientKey")),
                                ObjectUtils.to(String.class, credentialsMap.get("tenant")),
                                ObjectUtils.to(String.class, credentialsMap.get("storageName")),
                                ObjectUtils.to(String.class, credentialsMap.get("storageKey")));
                        break;

                    default:
                        EnterpriseException ee = new EnterpriseException(exchangeMap);
                        throw ee;
                }

            } catch (IOException error) {
                throw Throwables.propagate(error);
            }
        }

        if (credentials == null) {
            throw new BeamException("Unable to find Azure credentials for account '" + account + "'");
        }

        return credentials;
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
    }

    @Override
    public InetAddress findGateway(BeamRuntime runtime) {
        return null;
    }

    public String getStorageAccountBase() {
        return "https://" + getCredentials().getStorageName() + ".blob.core.windows.net/vhds/";
    }

    public String getStorageImagePath() {
        return "https://" + getCredentials().getStorageName() + ".blob.core.windows.net/system/Microsoft.Compute/Images/images/";
    }

    @Override
    public String copyDeploymentFile(String bucketName, String bucketRegion, String buildsKey, String oldBuildsKey, String commonKey, Object pending) {
        String jenkinsBucket = bucketName;
        BlobContainerResource containerResource = null;
        for (BlobContainerResource container : ((AzureCloudConfig)pending).getBlobContainers()) {
            if (container.getName().equals(jenkinsBucket)) {
                containerResource = container;

                break;
            }
        }

        if (containerResource == null) {
            containerResource = new BlobContainerResource();

            ((AzureCloudConfig)pending).getBlobContainers().add(containerResource);
            containerResource.setName(bucketName);
        }

        BlockBlobResource warResource = new BlockBlobResource();
        warResource.setContainer(warResource.newReference(containerResource));

        containerResource.getBlockBlobs().add(warResource);
        warResource.setKey(buildsKey);

        String containerUri = "https://" + this.getCredentials().getStorageName() + ".blob.core.windows.net/" + jenkinsBucket;
        String contentUrl = String.format("%s/%s", containerUri, commonKey);
        warResource.setObjectContentUrl(contentUrl);

        return String.format("%s/%s", containerUri, buildsKey);
    }

    @Override
    public void consoleLogin(boolean readonly, boolean urlOnly, PrintWriter out) throws Exception {
        String consoleUrl = "https://portal.azure.com/";
        if (urlOnly) {
            out.write(consoleUrl + "\n");
            out.flush();
        } else {
            Desktop.getDesktop().browse(new URI(consoleUrl));
        }
    }
}
