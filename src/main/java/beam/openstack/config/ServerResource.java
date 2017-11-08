package beam.openstack.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.config.ProvisionerConfig;
import beam.diff.NullArrayList;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Multimap;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.domain.VolumeAttachment;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.BlockDeviceMapping;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.nova.v2_0.predicates.ServerPredicates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerResource extends OpenStackResource<Server> {

    public static final String PUBLIC_NET = "00000000-0000-0000-0000-000000000000";
    public static final String SERVICE_NET = "11111111-1111-1111-1111-111111111111";

    private String serverId;
    private String name;
    private String flavor;
    private String image;
    private String keyPair;
    private String privateIP;
    private String publicIP;
    private boolean isPublicAccessible = true;
    private boolean isServiceNetAccessible = true;
    private Date created;
    private Integer beamLaunchIndex;
    private Map<String, String> metadata;
    private String userData;
    private BeamReference network;
    private BeamReference port;
    private List<ProvisionerConfig> provisioners;
    private List<CinderResource> volumes;
    private List<String> hostnames;
    private List<String> privateHostnames;

    private List<Network> networks;
    private Server server;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public void setInstanceType(String instanceType) {
        this.flavor = instanceType;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(String keyPair) {
        this.keyPair = keyPair;
    }

    public String getPrivateIP() {
        return privateIP;
    }

    public void setPrivateIP(String privateIP) {
        this.privateIP = privateIP;
    }

    public void setIpAddress(String ip) {
        this.privateIP = ip;
    }

    public String getPublicIP() {
        return publicIP;
    }

    public void setPublicIP(String publicIP) {
        this.publicIP = publicIP;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isPublicAccessible() {
        return isPublicAccessible;
    }

    public void setPublicAccessible(boolean isPublicAccessible) {
        this.isPublicAccessible = isPublicAccessible;
    }

    public boolean isServiceNetAccessible() {
        return isServiceNetAccessible;
    }

    public void setServiceNetAccessible(boolean isServiceNetAccessible) {
        this.isServiceNetAccessible = isServiceNetAccessible;
    }

    public BeamReference getNetwork() {
        return network;
    }

    public void setNetwork(BeamReference network) {
        this.network = network;
    }

    public BeamReference getPort() {
        return port;
    }

    public void setPort(BeamReference port) {
        this.port = port;
    }

    public Date getCreated() {
        if (created == null) {
            created = new Date();
        }

        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getBeamLaunchIndex() {
        return beamLaunchIndex;
    }

    public void setBeamLaunchIndex(Integer beamLaunchIndex) {
        this.beamLaunchIndex = beamLaunchIndex;
    }

    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public List<Network> getNetworks() {
        if (networks == null) {
            networks = new ArrayList<>();
        }

        return networks;
    }

    public List<ProvisionerConfig> getProvisioners() {
        return provisioners;
    }

    public void setProvisioners(List<ProvisionerConfig> provisioners) {
        this.provisioners = provisioners;
    }

    @ResourceDiffProperty(updatable = true)
    public List<CinderResource> getVolumes() {
        if (volumes == null) {
            volumes = new NullArrayList<>();
        }
        return volumes;
    }

    public void setVolumes(List<CinderResource> volumes) {
        this.volumes = volumes;
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

    public List<String> getPrivateHostnames() {
        if (privateHostnames == null) {
            privateHostnames = new ArrayList<>();
        }

        return privateHostnames;
    }

    public void setPrivateHostnames(List<String> privateHostnames) {
        this.privateHostnames = privateHostnames;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getMetadata().get("layer") + " " + getBeamLaunchIndex());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Server server) {
        setName(server.getName());
        setServerId(server.getId());
        setFlavor(server.getFlavor().getId());
        setMetadata(server.getMetadata());
        if (server.getImage() != null) {
            setImage(server.getImage().getId());
        }

        NetworkResource networkResource = (NetworkResource) getNetwork().resolve();

        Multimap<String, Address> addresses = server.getAddresses();
        for (String networkName : addresses.keySet()) {
            if ("public".equals(networkName)) {
                for (Object o : addresses.get(networkName)) {
                    Address address = (Address) o;
                    if (address.getVersion() == 4) {
                        setPublicIP(address.getAddr());
                        setPublicAccessible(true);
                        break;
                    }
                }
            } else if (networkName.equals(networkResource.getName())) {
                for (Object o : addresses.get(networkName)) {
                    Address address = (Address) o;
                    setPrivateIP(address.getAddr());
                }
            }
        }

        if (getPublicIP() == null) {
            setPublicAccessible(false);
        }

        CinderApi cinderApi = cloud.createCinderApi();
        VolumeApi volumeApi = cinderApi.getVolumeApi(getRegion());

        for (Volume volume : volumeApi.listInDetail()) {
            for (VolumeAttachment attachment : volume.getAttachments()) {
                if (attachment.getServerId().equals(getServerId())) {
                    CinderResource cinderResource = new CinderResource();

                    cinderResource.init(cloud, filter, volume);
                    getVolumes().add(cinderResource);
                }
            }
        }

        for (PortResource portResource : networkResource.getPorts()) {
            if (portResource.getDeviceId() != null &&
                    portResource.getDeviceId().equals(getServerId())) {
                setPort(newReference(portResource));
            }
        }

        this.server = server;
    }

    @Override
    public void create(OpenStackCloud cloud) {
        NovaApi api = cloud.createApi();
        ServerApi serverApi = api.getServerApi(getRegion());

        NetworkResource networkResource = (NetworkResource) getNetwork().resolve();
        Network network = networkResource.getNetwork();

        Set<BlockDeviceMapping> deviceMappings = new HashSet<>();
        for (CinderResource cinderResource : getVolumes()) {
            BlockDeviceMapping.Builder builder = BlockDeviceMapping.builder()
                    .volumeSize(cinderResource.getSize())
                    .deviceName(cinderResource.getDeviceName())
                    .destinationType("volume")
                    .bootIndex(0);

            if (cinderResource.getImage() != null) {
                builder.sourceType("image");
                builder.uuid(cinderResource.getImage());
            } else if (cinderResource.getSnapshotId() != null) {
                builder.uuid(cinderResource.getSnapshotId());
                builder.sourceType("snapshot");
            } else {
                builder.sourceType("blank");
            }

            BlockDeviceMapping deviceMapping = builder.build();

            deviceMappings.add(deviceMapping);

            if (cinderResource.isBootDevice()) {
                setImage("");
            }
        }

        List<org.jclouds.openstack.nova.v2_0.domain.Network> novaNetworks = new ArrayList<>();

        if (getPort() != null) {
            PortResource port = (PortResource) getPort().resolve();
            novaNetworks.add(buildNovaNetworkWithPort(network.getId(), port.getPortId()));
        }

        if (network != null) {
            if (getPort() != null) {
                PortResource port = (PortResource) getPort().resolve();
                novaNetworks.add(buildNovaNetworkWithPort(network.getId(), port.getPortId()));

                NetworkResource portNetwork = (NetworkResource) port.getNetwork().resolve();
                if (!portNetwork.getNetwork().getId().equals(network.getId())) {
                    novaNetworks.add(buildNovaNetwork(network.getId()));
                }
            } else {
                novaNetworks.add(buildNovaNetwork(network.getId()));
            }
        }

        if (isPublicAccessible()) {
            novaNetworks.add(buildNovaNetwork(PUBLIC_NET));
        }

        if (isServiceNetAccessible()) {
            novaNetworks.add(buildNovaNetwork(SERVICE_NET));
        }

        CreateServerOptions options = new CreateServerOptions()
                .configDrive(true)
                .keyPairName(getKeyPair())
                .metadata(getMetadata())
                .novaNetworks(novaNetworks)
                .blockDeviceMappings(deviceMappings)
                .userData(userData.getBytes());

        ServerCreated created = serverApi.create(getName(), getImage(), getFlavor(), options);

        while (!ServerPredicates.awaitStatus(serverApi, Server.Status.ACTIVE, 10L, 10L).apply(created.getId())) {
            server = serverApi.get(created.getId());
            if (server.getStatus() == Server.Status.ERROR) {
                throw new BeamException("Server failed to build.");
            } else if (server.getStatus() == Server.Status.BUILD) {
                continue;
            }

            init(cloud, null, server);
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Server> current, Set<String> changedProperties) {
        NovaApi api = cloud.createApi();
        ServerApi serverApi = api.getServerApi(getRegion());

        serverApi.rename(getServerId(), getName());
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        NovaApi api = cloud.createApi();
        ServerApi serverApi = api.getServerApi(getRegion());

        serverApi.delete(getServerId());
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getVolumes());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Server> current) throws Exception {
        ServerResource currentServer = (ServerResource) current;

        update.update(currentServer.getVolumes(), getVolumes());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getVolumes());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getMetadata().get("layer"));
        sb.append(" layer instance");

        if (serverId != null) {
            sb.append(" [" + serverId + "]");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getServerId();
    }

    private org.jclouds.openstack.nova.v2_0.domain.Network buildNovaNetworkWithPort(String uuid, String port) {
        org.jclouds.openstack.nova.v2_0.domain.Network.Builder builder =
                org.jclouds.openstack.nova.v2_0.domain.Network.builder()
                        .networkUuid(uuid);

        if (port != null) {
            builder.portUuid(port);
        }

        return builder.build();
    }

    private org.jclouds.openstack.nova.v2_0.domain.Network buildNovaNetwork(String uuid) {
        return buildNovaNetworkWithPort(uuid, null);
    }
}
