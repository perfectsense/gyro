package beam.openstack;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamInstance;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamRuntime;
import beam.config.AutoScaleConfig;
import beam.config.AutoScalePolicyConfig;
import beam.config.CloudConfig;
import beam.config.DeploymentConfig;
import beam.config.LayerConfig;
import beam.config.NetworkConfig;
import beam.config.PlacementConfig;
import beam.config.RootConfig;
import beam.config.VolumeConfig;
import beam.diff.Change;
import beam.diff.Diff;
import beam.diff.ResourceDiff;
import beam.diff.ResourceUpdate;
import beam.enterprise.EnterpriseApi;
import beam.enterprise.EnterpriseException;
import beam.openstack.config.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import com.google.inject.Module;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;

import org.jclouds.openstack.keystone.v2_0.KeystoneApi;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.jclouds.openstack.keystone.v2_0.features.TenantApi;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;

public class OpenStackCloud extends BeamCloud {

    private static final int CONTAINER_NAME_MAXIMUM = 63;

    private String project;
    private String account;
    private String serial;

    private String defaultRegion;
    OpenStackCredentials credentials;

    /**
     * @param runtime Can't be {@code null}.
     */
    public OpenStackCloud(BeamRuntime runtime) {
        Preconditions.checkNotNull(runtime, "runtime");

        this.project = runtime.getProject();
        this.serial = runtime.getSerial();
        this.account = runtime.getAccount();
    }

    public OpenStackCloud(String project, String serial) {
        Preconditions.checkNotNull(project, "project");
        Preconditions.checkNotNull(serial, "serial");

        this.project = project;
        this.serial = serial;
        this.account = "default";
    }

    public OpenStackCredentials getCredentials() {
        if (credentials != null) {
            return credentials;
        }

        File credentialsPath = new File(System.getProperty("user.home") +  File.separator + ".beam" + File.separator + "openstack.yml");
        if (!credentialsPath.exists()) {
            credentialsPath = new File("/etc/beam/openstack.yml");
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
                                credentials = new OpenStackCredentials(
                                        (String) accountMap.get("username"),
                                        (String) accountMap.get("password"),
                                        (String) accountMap.get("apiKey"));
                                break;
                            }
                        }
                    }
                }
            } catch(Exception ex) {
                throw new BeamException("Error trying to load OpenStack credentials for account '" + account + "'", ex);
            }
        }

        if (credentials == null && EnterpriseApi.isAvailable()) {
            try {
                Map<String, Object> exchangeMap = EnterpriseApi.call(
                        "rackspace/exchange-session",
                        new BasicNameValuePair("accountName", account),
                        new BasicNameValuePair("projectName", project));

                switch (ObjectUtils.to(String.class, exchangeMap.get("status"))) {
                    case "ok":
                        @SuppressWarnings("unchecked")
                        Map<String, Object> credentialsMap = (Map<String, Object>) exchangeMap.get("credentials");

                        credentials = new OpenStackCredentials(
                                ObjectUtils.to(String.class, credentialsMap.get("username")),
                                ObjectUtils.to(String.class, credentialsMap.get("password")),
                                ObjectUtils.to(String.class, credentialsMap.get("apiKey")));
                        break;

                    default:
                        throw new EnterpriseException(exchangeMap);
                }

            } catch (IOException error) {
                throw Throwables.propagate(error);
            }
        }

        if (credentials == null) {
            throw new BeamException("Unable to find OpenStack credentials for account '" + account + "'");
        }

        return credentials;
    }

    private ContextBuilder createContextBuilder(String providerOrApi) {
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(providerOrApi).
                modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()));

        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "20000");
        overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, "20000");

        OpenStackCredentials credentials = getCredentials();
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (ObjectUtils.isBlank(password)) {
            contextBuilder.credentials(username, credentials.getApiKey());

        } else {
            contextBuilder.credentials(username, password);

            overrides.put(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        }

        contextBuilder.overrides(overrides);

        return contextBuilder;
    }

    public NovaApi createApi() {
        return createContextBuilder("rackspace-cloudservers-us").buildApi(NovaApi.class);
    }

    public KeystoneApi createKeystonApi() {
        return createContextBuilder("openstack-keystone")
                .endpoint("https://identity.api.rackspacecloud.com/v2.0")
                .buildApi(KeystoneApi.class);
    }

    public CloudLoadBalancersApi createCloudLoadBalancersApi() {
        return createContextBuilder("rackspace-cloudloadbalancers-us").buildApi(CloudLoadBalancersApi.class);
    }

    public CloudDNSApi createCloudDnsApi() {
        return createContextBuilder("rackspace-clouddns-us").buildApi(CloudDNSApi.class);
    }

    public AutoscaleApi createAutoscaleApi() {
        return createContextBuilder("rackspace-autoscale-us").buildApi(AutoscaleApi.class);
    }

    public CloudFilesApi createCloudFilesApi() {
        return createContextBuilder("rackspace-cloudfiles-us").buildApi(CloudFilesApi.class);
    }

    public NeutronApi createNeutronApi() {
        return createContextBuilder("rackspace-cloudnetworks-us").buildApi(NeutronApi.class);
    }

    public CinderApi createCinderApi() {
        return createContextBuilder("rackspace-cloudblockstorage-us").buildApi(CinderApi.class);
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    @Override
    public String getName() {
        return "openstack";
    }

    private List<OpenStackInstance> getInstancesFromEnterprise(boolean cacheOk) {
        List<OpenStackInstance> instances = new ArrayList<OpenStackInstance>();

        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        try {
            Map<String, Object> instancesMap = EnterpriseApi.call("rackspace/instances",
                    new BasicNameValuePair("accountName", runtime.getAccount()),
                    new BasicNameValuePair("projectName", runtime.getProject()),
                    new BasicNameValuePair("serial", runtime.getSerial()),
                    new BasicNameValuePair("refresh", (!cacheOk ? "true" : "false")));

            String status = ObjectUtils.to(String.class, instancesMap.get("status"));

            if (status != null) {
                throw new BeamException("Invalid request: " + status);
            }

            for (String instanceId : instancesMap.keySet()) {
                OpenStackInstance instance = new OpenStackInstance(this, (Map<String, Object>) instancesMap.get(instanceId));
                for (String activeRegion : getActiveRegions()) {
                    if (activeRegion.equalsIgnoreCase(instance.getRegion())) {
                        instances.add(instance);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new BeamException("Failed to retrieve instances from Beam Enteprise.");
        }

        Collections.sort(instances);

        return instances;
    }

    @Override
    public List<? extends BeamInstance> getInstances(boolean cacheOk) throws Exception {
        if (EnterpriseApi.isAvailable()) {
            return getInstancesFromEnterprise(cacheOk);
        }

        NovaApi api = createApi();

        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        List<OpenStackInstance> instances = new ArrayList<>();
        for (String region : api.getConfiguredRegions()) {
            ServerApi serverApi = api.getServerApi(region);
            for (Server server : serverApi.listInDetail().concat()) {
                Map<String, String> metadata = server.getMetadata();

                if (ObjectUtils.isBlank(metadata)) {
                    continue;
                }

                if (!runtime.getProject().equals(metadata.get("project")) ||
                        !runtime.getSerial().equals(metadata.get("serial"))) {
                    continue;
                }

                OpenStackInstance instance = new OpenStackInstance(this, server, region);
                instances.add(instance);
            }
        }

        return instances;
    }

    @Override
    public List<Diff<?, ?, ?>> findChanges(BeamRuntime runtime) throws Exception {
        RootConfig config = runtime.getConfig();

        if (config == null) {
            return null;
        }

        NetworkConfig networkConfig = config.getNetworkConfig();
        for (CloudConfig cloudConfig : networkConfig.getClouds()) {
            if (!(cloudConfig instanceof OpenStackCloudConfig)) {
                continue;
            }

            // Read the current state from OpenStack service.
            OpenStackCloudConfig current = new OpenStackCloudConfig();
            OpenStackCloudConfig pending = (OpenStackCloudConfig) cloudConfig;

            // Override global account name.
            if (pending.getAccount() != null) {
                this.account = pending.getAccount();
            }

            OpenStackProjectFilter filter = new OpenStackProjectFilter();
            filter.setIncludedLayers(getIncludedLayers());
            filter.setHostnames(findHostnames(pending));

            current.setFilter(filter);
            current.init(this, pending.getActiveRegions());

            // DNS domain.
            String domain = pending.getSubdomain();
            if (domain == null) {
                domain = networkConfig.getSubdomain();
            }

            DomainResource domainResource = new DomainResource();
            domainResource.setName(domain);
            domainResource.setEmail(pending.getSubdomainEmail());
            pending.getDomains().add(domainResource);

            Map<String, DeploymentResource> deploymentResources = new HashMap<>();

            // Read the pending state from local configuration files.
            for (RegionResource region : pending.getRegions()) {
                String openstackRegion = region.getName().toUpperCase();

                // Pre-fetch the image info.
                Map<String, Image> images = new HashMap<>();
                Set<String> imageIds = new HashSet<>();
                Set<String> imageNames = new HashSet<>();

                for (NetworkResource subnet : region.getSubnets()) {
                    ServerResource gateway = subnet.getGateway();

                    if (gateway != null) {
                        addImageIdOrName(imageIds, imageNames, gateway.getImage());
                    }
                }

                for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    addImageIdOrName(imageIds, imageNames, layer.getImage());
                }

                if (!imageIds.isEmpty()) {
                    populateImages(images, openstackRegion, null);
                }

                if (!imageNames.isEmpty()) {
                    populateImages(images, openstackRegion, imageNames);
                }

                // Translate region config to network resources.
                String project = runtime.getProject();
                String environment = runtime.getEnvironment();
                String serial = runtime.getSerial();
                String internalDomain = runtime.getInternalDomain();
                String sandbox = String.valueOf(networkConfig.isSandbox());

                // Key pair for network resource.
                String keyName = String.format("%s-%s", project, openstackRegion);
                boolean needKey = true;
                for (KeyPairResource kpResource : region.getKeyPairs()) {
                    if (kpResource.getKeyName().equals(keyName)) {
                        kpResource.setRegion(openstackRegion);
                        needKey = false;
                        break;
                    }
                }

                if (needKey) {
                    KeyPairResource kpResource = new KeyPairResource();
                    kpResource.setRegion(openstackRegion);
                    kpResource.setKeyName(keyName);

                    region.getKeyPairs().add(kpResource);
                }

                // Make sure region is set on swift containers.
                for (SwiftContainerResource containerResource : region.getContainers()) {
                    containerResource.setRegion(openstackRegion);
                }

                // Cloud Load Balancers
                for (LoadBalancerResource loadBalancerResource : region.getLoadBalancers()) {
                    String lbName = String.format("%s %s v%s",
                            loadBalancerResource.getName(),
                            project, serial);

                    loadBalancerResource.setName(lbName);
                    loadBalancerResource.setRegion(openstackRegion);

                    if (loadBalancerResource.getHealthMonitor() != null) {
                        HealthMonitorResource healthMonitorResource = loadBalancerResource.getHealthMonitor();
                        healthMonitorResource.setRegion(openstackRegion);
                        healthMonitorResource.setLoadBalancer(healthMonitorResource.newReference(loadBalancerResource));
                    }

                    if (loadBalancerResource.getHostnames().size() > 0) {
                        for (String hostname : loadBalancerResource.getHostnames()) {
                            if (!hostname.endsWith(".")) {
                                hostname = hostname + "." + domain;
                            } else {
                                hostname = hostname.substring(0, hostname.length() - 1);
                            }

                            DomainRecordResource recordResource = new DomainRecordResource();
                            domainResource.getRecords().add(recordResource);
                            recordResource.setName(hostname);
                            recordResource.setTTL(300);
                            recordResource.setType("A");

                            DomainRecordResource.ReferenceValue value =
                                    new DomainRecordResource.ReferenceValue(recordResource.newReference(loadBalancerResource));
                            value.setType("A");

                            recordResource.setValue(value);
                        }
                    }
                }

                // Subnets in the network.
                Map<String, List<NetworkResource>> subnetResourcesByType = new HashMap<>();
                for (NetworkResource subnet : region.getSubnets()) {
                    StringBuilder networkName = new StringBuilder();
                    networkName.append(project);
                    networkName.append(" v");
                    networkName.append(runtime.getSerial());

                    if (!subnet.getTypes().isEmpty()) {
                        networkName.append(" [");
                        networkName.append(subnet.getTypes().iterator().next());
                        networkName.append("]");
                    }

                    // Map subnet types to beam groups.
                    for (String subnetType : subnet.getTypes()) {
                        List<NetworkResource> networkResources = subnetResourcesByType.get(subnetType);

                        if (networkResources == null) {
                            networkResources = new ArrayList<>();
                            subnetResourcesByType.put(subnetType, networkResources);
                        }

                        networkResources.add(subnet);
                    }

                    subnet.setName(networkName.toString());
                    subnet.setRegion(openstackRegion);

                    Collections.sort(subnet.getDnsNameServers());

                    // Gateway instance.
                    ServerResource gatewayResource = subnet.getGateway();
                    if (gatewayResource != null &&
                            (getIncludedLayers().size() == 0 || getIncludedLayers().contains("gateway"))) {
                        Map metadata = new ImmutableMap.Builder<>().
                                put("project", project).
                                put("environment", environment).
                                put("serial", serial).
                                put("internalDomain", internalDomain).
                                put("sandbox", sandbox).
                                put("layer", "gateway").
                                put("build", "prod").
                                build();

                        String userData = BaseEncoding.base64().
                                encode(ObjectUtils.toJson(metadata).getBytes(StringUtils.UTF_8));

                        gatewayResource.setName(String.format("%s network serial-%s gateway", project, serial));
                        gatewayResource.setMetadata(metadata);
                        gatewayResource.setUserData(userData);
                        gatewayResource.setRegion(openstackRegion);
                        gatewayResource.setNetwork(gatewayResource.newReference(subnet));
                        gatewayResource.setImage(findImageId(images, gatewayResource.getImage()));
                        gatewayResource.setKeyPair(keyName);
                        gatewayResource.setBeamLaunchIndex(0);

                        // Create a network port for this IP and assign it to the instance.
                        if (gatewayResource.getPrivateIP() != null) {
                            PortResource portResource = new PortResource();
                            portResource.setRegion(openstackRegion);
                            portResource.setName(String.format("%s network serial-%s gateway", project, serial));
                            portResource.setIp(gatewayResource.getPrivateIP());
                            portResource.setNetwork(portResource.newReference(subnet));

                            subnet.getPorts().add(portResource);
                            gatewayResource.setPort(gatewayResource.newReference(portResource));
                        }

                        GatewayRouteResource gatewayRouteResource = new GatewayRouteResource();
                        gatewayRouteResource.setRegion(openstackRegion);
                        gatewayRouteResource.setNetwork(gatewayRouteResource.newReference(subnet));
                        gatewayRouteResource.setGateway(gatewayRouteResource.newReference(gatewayResource));

                        region.getGatewayRoutes().add(gatewayRouteResource);

                        // Configure hostnames.
                        if (gatewayResource.getHostnames().size() > 0) {
                            for (String hostname : gatewayResource.getHostnames()) {
                                if (!hostname.endsWith(".")) {
                                    hostname = hostname + "." + domain;
                                } else {
                                    hostname = hostname.substring(0, hostname.length() - 1);
                                }

                                DomainRecordResource recordResource = new DomainRecordResource();
                                domainResource.getRecords().add(recordResource);
                                recordResource.setName(hostname);
                                recordResource.setTTL(300);
                                recordResource.setType("A");

                                DomainRecordResource.ReferenceValue value =
                                        new DomainRecordResource.ReferenceValue(recordResource.newReference(gatewayResource));
                                value.setType("A");

                                recordResource.setValue(value);
                            }
                        }

                        if (gatewayResource.getPrivateHostnames().size() > 0) {
                            for (String hostname : gatewayResource.getPrivateHostnames()) {
                                if (!hostname.endsWith(".")) {
                                    hostname = hostname + "." + domain;
                                } else {
                                    hostname = hostname.substring(0, hostname.length() - 1);
                                }

                                DomainRecordResource recordResource = new DomainRecordResource();
                                domainResource.getRecords().add(recordResource);
                                recordResource.setName(hostname);
                                recordResource.setTTL(300);
                                recordResource.setType("A");

                                DomainRecordResource.ReferenceValue value =
                                        new DomainRecordResource.ReferenceValue(recordResource.newReference(gatewayResource));
                                value.setPrivate(true);
                                value.setType("A");

                                recordResource.setValue(value);
                            }
                        }
                    } else {
                        subnet.setGateway(null);
                    }
                }

                LAYERS: for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    // Copy deployment WAR file to S3.
                    DeploymentConfig deployment = layer.getDeployment();
                    String buildPath = null;
                    String buildNumber = null;
                    Map<String, String> dataMap = new HashMap<>();

                    if (deployment != null) {
                        dataMap = deployment.prepare(this, region);
                    }

                    String layerImageId = findImageId(images, layer.getImage());
                    String layerName = layer.getName();

                    Map<String, String> baseMetadata = new HashMap<>();
                    baseMetadata.put("project", project);
                    baseMetadata.put("environment", environment);
                    baseMetadata.put("serial", serial);
                    baseMetadata.put("internalDomain", internalDomain);
                    baseMetadata.put("sandbox", sandbox);

                    if (deployment != null) {
                        baseMetadata.putAll(deployment.getGroupHashItems());
                    }

                    Map metadata = new ImmutableMap.Builder<>().
                            putAll(baseMetadata).
                            put("layer", layerName).
                            putAll(dataMap).build();

                    String userData = BaseEncoding.base64().
                            encode(ObjectUtils.toJson(metadata).getBytes(StringUtils.UTF_8));

                    for (PlacementConfig placement : layer.getPlacements()) {
                        String subnetType = placement.getSubnetType();
                        List<NetworkResource> networkResources = subnetResourcesByType.get(subnetType);

                        if (networkResources == null) {
                            continue;
                        }

                        AutoScaleConfig as = (AutoScaleConfig) placement.getAutoscale();
                        int subnetResourcesSize = networkResources.size();

                        // Static number of instances.
                        if (as == null) {
                            int placementSizePerSubnet = placement.getSizePerSubnet();

                            // Configure hostnames.
                            List<DomainRecordResource> hostnameResources = new ArrayList<>();
                            if (placement.getHostnames().size() > 0) {
                                for (String hostname : placement.getHostnames()) {
                                    DomainRecordResource recordResource = new DomainRecordResource();

                                    if (!hostname.endsWith(".")) {
                                        hostname = hostname + "." + domain;
                                    } else {
                                        hostname = hostname.substring(0, hostname.length() - 1);
                                    }

                                    domainResource.getRecords().add(recordResource);
                                    recordResource.setName(hostname);
                                    recordResource.setTTL(300);
                                    recordResource.setType("A");
                                    recordResource.setDomain(recordResource.newReference(domainResource));

                                    hostnameResources.add(recordResource);
                                }
                            }

                            for (NetworkResource networkResource : networkResources) {
                                Integer beamLaunchIndex = 0;

                                for (int i = 0; i < placementSizePerSubnet; i++) {
                                    ServerResource serverResource = new ServerResource();
                                    serverResource.setRegion(openstackRegion);
                                    serverResource.setFlavor(layer.getInstanceType());
                                    serverResource.setImage(layerImageId);
                                    serverResource.setBeamLaunchIndex(beamLaunchIndex++);
                                    serverResource.setMetadata(metadata);
                                    serverResource.setNetwork(serverResource.newReference(networkResource));
                                    serverResource.setName(String.format("%s %s serial-%s %s", project, environment, serial, layerName));
                                    serverResource.setPublicAccessible(networkResource.isPublicAccessible());
                                    serverResource.setServiceNetAccessible(networkResource.isServiceNetAccessible());
                                    serverResource.setKeyPair(keyName);
                                    serverResource.setUserData(userData);

                                    networkResource.getServers().add(serverResource);

                                    for (DomainRecordResource recordResource : hostnameResources) {
                                        DomainRecordResource.ReferenceValue value =
                                                new DomainRecordResource.ReferenceValue(recordResource.newReference(serverResource));
                                        value.setType("A");

                                        recordResource.setValue(value);
                                    }

                                    // Cloud Block Storage Volumes.
                                    CinderResource bootVolume = null;
                                    for (VolumeConfig volumeConfig : layer.getVolumes()) {
                                        String volumeName = String.format("%s %s %s",
                                                layerName, project, volumeConfig.getName());

                                        CinderResource cinderResource = new CinderResource();
                                        cinderResource.setDeleteOnTerminate(volumeConfig.getDeleteOnTerminate());
                                        cinderResource.setDeviceName(volumeConfig.getDeviceName());
                                        cinderResource.setVolumeType(volumeConfig.getVolumeType());
                                        cinderResource.setName(volumeName);
                                        cinderResource.setMetadata(metadata);

                                        if (volumeConfig.getSize() != null) {
                                            cinderResource.setSize(volumeConfig.getSize());
                                        }

                                        if (volumeConfig.getImage() != null) {
                                            cinderResource.setImage(volumeConfig.getImage());
                                        }

                                        if (volumeConfig.getName().equals("boot") && volumeConfig.getSnapshotId() == null) {
                                            cinderResource.setBootDevice(true);
                                            cinderResource.setImage(layerImageId);

                                            bootVolume = cinderResource;
                                        } else if (volumeConfig.getSnapshotId() != null) {
                                            cinderResource.setSnapshotId(volumeConfig.getSnapshotId());
                                        }

                                        serverResource.getVolumes().add(cinderResource);
                                    }

                                    // Handle booting a compute1 or memory1 instance
                                    // using an image.
                                    if (layer.getInstanceType().startsWith("compute1") ||
                                            layer.getInstanceType().startsWith("memory1")) {
                                        serverResource.setImage("");

                                        if (bootVolume == null) {
                                            bootVolume = new CinderResource();

                                            bootVolume.setName("boot");
                                            bootVolume.setBootDevice(true);
                                            bootVolume.setDeleteOnTerminate(true);
                                            bootVolume.setVolumeType("SSD");
                                            bootVolume.setMetadata(metadata);
                                            bootVolume.setImage(layerImageId);

                                            serverResource.getVolumes().add(bootVolume);
                                        }

                                        if (bootVolume.getSize() == null) {
                                            Image image = images.get(bootVolume.getImage());
                                            bootVolume.setSize(image.getMinDisk());
                                        }
                                    }
                                }
                            }
                        } else {
                            StringBuilder hashBuilder = new StringBuilder();

                            Map<String, String> autoscaleMetadata = new HashMap();
                            autoscaleMetadata.putAll(metadata);
                            autoscaleMetadata.put("autoscale", "true");

                            AutoscaleResource autoscaleResource = new AutoscaleResource();
                            autoscaleResource.setRegion(openstackRegion);
                            autoscaleResource.setMinEntities(as.getMinPerSubnet());
                            autoscaleResource.setMaxEntities(as.getMaxPerSubnet());
                            autoscaleResource.setCoolDown(as.getCooldown());
                            autoscaleResource.setMetadata(new HashMap(baseMetadata));

                            LaunchConfigurationResource lcResource = new LaunchConfigurationResource();
                            lcResource.setImage(appendHash(hashBuilder, "image", layerImageId));
                            lcResource.setFlavor(appendHash(hashBuilder, "instanceType", layer.getInstanceType()));
                            lcResource.setMetadata(autoscaleMetadata);
                            lcResource.setUserData(appendHash(hashBuilder, "userData", userData));
                            lcResource.getNetworks().add(autoscaleResource.newReference(networkResources.get(0)));
                            lcResource.setRegion(openstackRegion);

                            // Attach Load Balancers.
                            for (LoadBalancerResource loadBalancerResource : region.getLoadBalancers()) {
                                for (String requestedLbName : as.getLoadBalancers()) {
                                    String actualLbName = String.format("%s %s v%s",
                                            requestedLbName, project, serial);

                                    if (loadBalancerResource.getName().equals(actualLbName)) {
                                        lcResource.getLoadBalancers().add(lcResource.newReference(loadBalancerResource));
                                    }
                                }
                            }

                            String hash = StringUtils.hex(StringUtils.md5(hashBuilder.toString())).substring(0, 8);

                            if (deployment != null) {
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
                            }

                            String asName = String.format(
                                    "%s %s %s v%s %s %s %s %s",
                                    project,
                                    layerName,
                                    environment,
                                    serial,
                                    layerImageId.split("-")[0],
                                    buildPath,
                                    buildNumber,
                                    hash);

                            lcResource.setServerName(asName);

                            autoscaleResource.setLaunchConfig(lcResource);
                            autoscaleResource.setName(asName);

                            StringBuilder groupHashBuilder = new StringBuilder();
                            appendHash(groupHashBuilder, "image", layerImageId);
                            appendHash(groupHashBuilder, "instanceType", layer.getInstanceType());

                            Map<String, String> groupHashItems = deployment.getGroupHashItems();
                            for (String key : groupHashItems.keySet()) {
                                String value = groupHashItems.get(key);
                                appendHash(groupHashBuilder, key, value);
                            }

                            String groupHash = StringUtils.hex(StringUtils.md5(groupHashBuilder.toString()));
                            autoscaleResource.setGroupHash(groupHash);

                            DeploymentResource deploymentResource = deploymentResources.get(groupHash);
                            if (deploymentResource == null) {
                                deploymentResource = new DeploymentResource();
                                deploymentResource.setRegion(openstackRegion);
                                deploymentResource.setGroupHash(groupHash);
                                deploymentResource.setImage(layerImageId);
                                deploymentResource.setInstanceType(layer.getInstanceType());
                                deploymentResource.setDeploymentString(deployment.toDisplayString());
                                deploymentResource.setDomain(deploymentResource.newReference(domainResource));
                                deploymentResources.put(groupHash, deploymentResource);

                                pending.getDeployments().add(deploymentResource);
                            }

                            deploymentResource.getAutoscaleGroups().add(autoscaleResource);

                            // Auto scaling group policies.
                            for (AutoScalePolicyConfig policy : as.getPolicies()) {
                                String policyName = project + "-" + policy.getName() + "-" + hash;
                                ScalingPolicyResource policyResource = new ScalingPolicyResource();

                                policyResource.setRegion(openstackRegion);
                                policyResource.setName(policyName);
                                policyResource.setTargetType("incremental");
                                policyResource.setCoolDown(policy.getCooldown());
                                policyResource.setTarget(String.valueOf(policy.getInstancesPerSubnet() * subnetResourcesSize));

                                // TODO: Add alarm trigger.

                                autoscaleResource.getPolicies().add(policyResource);
                            }

                        }
                    }
                }
            }

            BeamResource.updateTree(current);
            BeamResource.updateTree(pending);

            List<Diff<?, ?, ?>> diffs = new ArrayList<>();
            ResourceDiff regionDiff = new ResourceDiff(
                    this,
                    pending.getFilter(),
                    RegionResource.class,
                    current.getRegions(),
                    pending.getRegions());

            regionDiff.diff();
            regionDiff.getChanges().clear();
            regionDiff.diff();
            diffs.add(regionDiff);

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

            ResourceDiff domainDiff = new ResourceDiff(
                    this,
                    pending.getFilter(),
                    DomainResource.class,
                    current.getDomains(),
                    pending.getDomains());

            domainDiff.diff();
            domainDiff.getChanges().clear();
            domainDiff.diff();
            diffs.add(domainDiff);

            tryToKeep(diffs);

            return diffs;

        }

        return null;
    }

    public List<String> normalizeHostnames(Collection<String> hostnames, String domain) {
        List<String> normalized = new ArrayList<>();

        if (hostnames.size() > 0) {
            for (String hostname : hostnames) {
                normalized.add(normalizeHostname(hostname, domain));
            }
        }

        return normalized;
    }

    public String normalizeHostname(String hostname, String domain) {
        if (!hostname.endsWith(".")) {
            hostname = hostname + "." + domain;
        } else {
            hostname = hostname.substring(0, hostname.length() - 1);
        }

        return hostname;
    }

    public Set<String> findHostnames(OpenStackCloudConfig pending) {
        RootConfig config = BeamRuntime.getCurrentRuntime().getConfig();

        Set<String> pendingHostnames = new HashSet<>();

        // DNS domain.
        String domain = pending.getSubdomain();
        if (domain == null) {
            domain = config.getNetworkConfig().getSubdomain();
        }

        // Read the pending state from local configuration files.
        for (RegionResource region : pending.getRegions()) {
            for (LoadBalancerResource loadBalancerResource : region.getLoadBalancers()) {
                pendingHostnames.addAll(normalizeHostnames(loadBalancerResource.getHostnames(), domain));
            }

            // Subnets in the network.
            Map<String, List<NetworkResource>> subnetResourcesByType = new HashMap<>();
            for (NetworkResource subnet : region.getSubnets()) {

                ServerResource gatewayResource = subnet.getGateway();
                if (gatewayResource != null && (getIncludedLayers().size() == 0 || getIncludedLayers().contains("gateway"))) {
                    pendingHostnames.addAll(normalizeHostnames(gatewayResource.getHostnames(), domain));
                    pendingHostnames.addAll(normalizeHostnames(gatewayResource.getPrivateHostnames(), domain));
                }
            }

            LAYERS: for (LayerConfig layer : config.getLayers()) {

                if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                    continue;
                }

                for (PlacementConfig placement : layer.getPlacements()) {
                    if (placement.getAutoscale() == null) {
                        pendingHostnames.addAll(normalizeHostnames(placement.getHostnames(), domain));
                    }
                }
            }
        }

        return pendingHostnames;
    }

    @Override
    public InetAddress findGateway(BeamRuntime runtime) {
        return null;
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        OpenStackCredentials creds = getCredentials();
        String apiKey = creds.getApiKey();
        String password = creds.getPassword();

        mapBuilder.put("username", creds.getUsername());

        if (!ObjectUtils.isBlank(apiKey)) {
            mapBuilder.put("apiKey", apiKey);
        }

        if (!ObjectUtils.isBlank(password)) {
            mapBuilder.put("password", password);
        }

        return mapBuilder.build();
    }

    public String findOldDefaultContainerName() {
        TenantApi tenantApi = createKeystonApi().getTenantApi().get();

        String tenantId = null;
        for (Tenant tenant : tenantApi.list().concat()) {
            if (!tenant.getId().startsWith("Mosso")) {
                tenantId = tenant.getName();
                break;
            }
        }

        if (tenantId != null) {
            BeamRuntime runtime = BeamRuntime.getCurrentRuntime();
            String containerName = String.format(
                    "%s-%s-%s-%s",
                    runtime.getProject(),
                    runtime.getEnvironment(),
                    runtime.getSerial(),
                    StringUtils.hex(StringUtils.sha1(tenantId)).substring(0, 8));

            if (containerName.length() > CONTAINER_NAME_MAXIMUM) {
                containerName = containerName.substring(0, CONTAINER_NAME_MAXIMUM);
            }

            return containerName;
        }

        throw new IllegalStateException("Can't infer Rackspace account ID!");
    }

    public boolean keyExists(String containerName, String region, String key) {
        CloudFilesApi api = createCloudFilesApi();
        ObjectApi objectApi = api.getObjectApi(region, containerName);

        SwiftObject swiftObject = objectApi.getWithoutBody(key);
        if (swiftObject == null) {
            return false;
        }

        return true;
    }

    private void addImageIdOrName(Set<String> imageIds, Set<String> imageNames, String image) {
        if (image != null) {
            if (image.startsWith("ami-")) {
                imageIds.add(image);

            } else {
                imageNames.add(image);
            }
        }
    }

    private void populateImages(Map<String, Image> images, String region, Set<String> filter) {
        ImageApi imageApi = createApi().getImageApi(region);

        for (Image image : imageApi.listInDetail().concat()) {
            if (filter != null) {
                for (String include : filter) {
                    if (image.getName().equals(include)) {
                        images.put(image.getId(), image);
                        images.put(image.getName(), image);
                    }
                }
            } else {
                images.put(image.getId(), image);
                images.put(image.getName(), image);
            }
        }
    }

    private String findImageId(Map<String, Image> images, String imageIdOrName) {
        Image image = images.get(imageIdOrName);

        if (image != null) {
            return image.getId();

        } else {
            return imageIdOrName;
        }
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
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

    @Override
    public String copyDeploymentFile(String containerName, String containerRegion, String buildsKey, String oldBuildsKey, String commonKey, Object pending) {
        String oldContainerName = findOldDefaultContainerName();
        SwiftContainerResource containerResource = null;
        RegionResource region = ((RegionResource)pending);
        String jenkinsBucket = containerName;

        String openstackRegion = region.getName().toUpperCase();

        if (keyExists(oldContainerName, openstackRegion, oldBuildsKey)) {
            containerName = findOldDefaultContainerName();
        }

        for (SwiftContainerResource c : region.getContainers()) {
            if (c.getName().equals(containerName)) {
                containerResource = c;

                break;
            }
        }

        if (containerResource == null) {
            containerResource = new SwiftContainerResource();
            containerResource.setRegion(region.getName());
            containerResource.setName(containerName);

            region.getContainers().add(containerResource);
        } else {
            containerResource.setRegion(region.getName());
        }

        SwiftObjectResource warResource = new SwiftObjectResource();
        if (containerRegion == null) {
            containerRegion = containerResource.getRegion();
        }

        if (keyExists(oldContainerName, openstackRegion, oldBuildsKey)) {
            containerName = oldContainerName;
            buildsKey = oldBuildsKey;
        }

        warResource.setPath(buildsKey);
        warResource.setObjectContentUrl(String.format("cloudfiles://%s/%s/%s",
                containerRegion, jenkinsBucket, commonKey));
        warResource.setContainer(containerResource.newReference(containerResource));

        containerResource.getSwiftObjects().add(warResource);

        return String.format("cloudfiles://%s/%s", containerName, buildsKey);
    }

    @Override
    public void consoleLogin(boolean readonly, boolean urlOnly, PrintWriter out) throws Exception {
        String consoleUrl = "https://mycloud.rackspace.com";
        if (urlOnly) {
            out.write(consoleUrl + "\n");
            out.flush();
        } else {

            Map<String, String> supportedDrivers = ImmutableMap.of(
                    "org.openqa.selenium.chrome.ChromeDriver", "io.github.bonigarcia.wdm.ChromeDriverManager",
                    "org.openqa.selenium.safari.SafariDriver", "");

            PrintStream originalStream = System.out;
            PrintStream silentStream = new PrintStream(new OutputStream() {
                public void write(int b) {

                }
            });

            OpenStackCredentials credentials = getCredentials();
            Iterator<String> iter = supportedDrivers.keySet().iterator();
            boolean success = false;
            while (iter.hasNext() && !success) {
                String driverName = iter.next();
                WebDriver driver = null;
                System.setOut(silentStream);
                System.setErr(silentStream);

                try {
                    if (!ObjectUtils.isBlank(supportedDrivers.get(driverName))) {
                        Class<?> driverManagerClass = Class.forName(supportedDrivers.get(driverName));
                        Method instanceMethod = driverManagerClass.getMethod("getInstance");
                        Method setupMethod = driverManagerClass.getMethod("setup");

                        Object driverManager = instanceMethod.invoke(null);
                        setupMethod.invoke(driverManager);
                    }

                    Class<?> driverClass = Class.forName(driverName);
                    driver = (WebDriver) driverClass.getConstructor().newInstance();

                    driver.get(consoleUrl);
                    WebElement username = driver.findElement(By.name("username"));
                    WebElement password = driver.findElement(By.name("password"));
                    username.sendKeys(credentials.getUsername());
                    password.sendKeys(credentials.getPassword());
                    username.submit();

                    System.setOut(originalStream);
                    System.setErr(originalStream);
                    BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));
                    out.format("Press any key to logout.");
                    out.flush();
                    confirmReader.readLine();
                    success = true;
                } catch (Exception e) {

                } finally {
                    System.setOut(originalStream);
                    System.setErr(originalStream);
                    if (driver != null) {
                        driver.quit();
                    }
                }
            }

            if (!success) {
                throw new BeamException("Unable to connect to rackspace console with " + String.join(" or ", supportedDrivers.keySet()));
            }
        }
    }
}
