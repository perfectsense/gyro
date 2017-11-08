package beam.openstack.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.NullArrayList;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.*;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.features.PortApi;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import java.util.*;

public class NetworkResource extends OpenStackResource<Network> {

    private String name;
    private String networkId;
    private String subnetId;
    private String cidr;
    private Set<String> types;
    private boolean isPublicAccessible;
    private boolean isServiceNetAccessible;
    private List<ServerResource> servers;
    private ServerResource gateway;
    private List<String> dnsNameServers;
    private Set<Pool> pools;
    private Set<PortResource> ports;

    private Network network;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public List<ServerResource> getServers() {
        if (servers == null) {
            servers = new ArrayList<>();
        }

        return servers;
    }

    public void setServers(List<ServerResource> servers) {
        this.servers = servers;
    }

    public ServerResource getGateway() {
        return gateway;
    }

    public void setGateway(ServerResource gateway) {
        this.gateway = gateway;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getDnsNameServers() {
        if (dnsNameServers == null) {
            dnsNameServers = new NullArrayList<>();
        }

        return dnsNameServers;
    }

    public void setDnsNameServers(List<String> dnsNameServers) {
        this.dnsNameServers = dnsNameServers;
    }

    public Set<AllocationPool> getAllocationPools() {
        Set<AllocationPool> ap = new HashSet<>();

        for (Pool pool : getPools()) {
            ap.add(pool.allocationPool());
        }

        return ap;
    }

    public Set<Pool> getPools() {
        if (pools == null) {
            pools = new HashSet<>();
        }

        return pools;
    }

    public void setPools(Set<Pool> pools) {
        this.pools = pools;
    }

    public Set<PortResource> getPorts() {
        if (ports == null) {
            ports = new HashSet();
        }

        return ports;
    }

    public void setPorts(Set<PortResource> ports) {
        this.ports = ports;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getCidr());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Network network) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        SubnetApi subnetApi = neutronApi.getSubnetApi(getRegion());

        setName(network.getName());
        setNetworkId(network.getId());
        setNetwork(network);

        for (String subnetId : network.getSubnets()) {
            Subnet subnet = subnetApi.get(subnetId);

            if (subnet != null && subnet.getIpVersion() != null && subnet.getIpVersion() == 4) {
                setSubnetId(subnetId);
                setCidr(subnet.getCidr());

                List<String> dns = new ArrayList<>(subnet.getDnsNameservers());
                Collections.sort(dns);
                setDnsNameServers(dns);

                break;
            }
        }

        if (filter != null) {
            PortApi portApi = neutronApi.getPortApi(getRegion());
            for (Port port : portApi.list().concat()) {
                if (port.getNetworkId().equals(getNetworkId()) && filter.isInclude(port)) {
                    PortResource portResource = new PortResource();
                    portResource.setRegion(getRegion());
                    portResource.init(cloud, filter, port);

                    getPorts().add(portResource);
                }
            }

            Map<String, List<Server>> serverByLayerName = new HashMap<>();

            NovaApi api = cloud.createApi();
            ServerApi serverApi = api.getServerApi(getRegion());

            for (Server server : serverApi.listInDetail().concat()) {
                Multimap<String, Address> addresses = server.getAddresses();

                for (String networkName : addresses.keySet()) {
                    if (networkName.equals(getName()) && filter.isInclude(server)) {

                        Map<String, String> metadata = server.getMetadata();
                        String layerName = metadata != null && metadata.get("layer") != null ? metadata.get("layer") : null;
                        boolean autoscale = metadata != null && metadata.get("layer") != null ? "true".equals(metadata.get("autoscale")) : false;

                        if (!autoscale) {
                            List<Server> servers = serverByLayerName.get(layerName);

                            if (servers == null) {
                                servers = new ArrayList<>();
                                serverByLayerName.put(layerName, servers);
                            }

                            servers.add(server);
                        }
                    }
                }
            }

            for (List<Server> servers : serverByLayerName.values()) {
                // Sort instances by create date, then server id.
                Collections.sort(servers, new Comparator<Server>() {
                    @Override
                    public int compare(Server o1, Server o2) {
                        if (o1.getCreated().before(o2.getCreated())) {
                            return -1;
                        }

                        if (o1.getCreated().after(o2.getCreated())) {
                            return 1;
                        }

                        if (o1.getId().equals(o2.getId())) {
                            return o1.getId().compareTo(o2.getId());
                        }

                        return 0;
                    }
                });

                Integer beamLaunchIndex = 0;
                for (Server server : servers) {
                    if (!filter.isInclude(server)) {
                        continue;
                    }

                    ServerResource serverResource = new ServerResource();
                    serverResource.setRegion(getRegion());
                    serverResource.setNetwork(serverResource.newReference(this));
                    serverResource.setBeamLaunchIndex(beamLaunchIndex++);
                    serverResource.init(cloud, filter, server);

                    if ("gateway".equals(server.getMetadata().get("layer"))) {
                        setGateway(serverResource);
                    } else {
                        getServers().add(serverResource);
                    }

                }
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getPorts());
        create.createOne(getGateway());
        create.create(getServers());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Network> current) throws Exception {
        NetworkResource currentNetwork = (NetworkResource) current;

        update.update(currentNetwork.getPorts(), getPorts());
        update.updateOne(currentNetwork.getGateway(), getGateway());
        update.update(currentNetwork.getServers(), getServers());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getPorts());
        delete.deleteOne(getGateway());
        delete.delete(getServers());
    }

    @Override
    public void create(OpenStackCloud cloud) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        NetworkApi networkApi = neutronApi.getNetworkApi(getRegion());

        Network.CreateNetwork createNetwork = Network.CreateNetwork.createBuilder(getName()).build();
        network = networkApi.create(createNetwork);

        setNetworkId(network.getId());

        while (network.getStatus() == NetworkStatus.BUILD) {
            network = networkApi.get(getNetworkId());

            try {
                Thread.sleep(1000);
            } catch(Exception ex) {
                throw new BeamException("Beam was interrupted while creating a network resource.");
            }
        }

        SubnetApi subnetApi = neutronApi.getSubnetApi(getRegion());

        Subnet.CreateBuilder subnetBuilder = Subnet.CreateSubnet
                .createBuilder(getNetworkId(), getCidr())
                .ipVersion(4)
                .name(getName());

        if (!getAllocationPools().isEmpty()) {
            subnetBuilder.allocationPools(getAllocationPools());
        }

        if (!getDnsNameServers().isEmpty()) {
            subnetBuilder.dnsNameServers(ImmutableSet.copyOf(getDnsNameServers()));
        }

        Subnet.CreateSubnet createSubnet = subnetBuilder.build();

        Subnet subnet = subnetApi.create(createSubnet);
        setSubnetId(subnet.getId());
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Network> current, Set<String> changedProperties) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        SubnetApi subnetApi = neutronApi.getSubnetApi(getRegion());

        Subnet.UpdateBuilder subnetBuilder = Subnet.UpdateSubnet.updateBuilder()
                .dnsNameServers(ImmutableSet.copyOf(getDnsNameServers()))
                .name(getName());

        Subnet.UpdateSubnet updateSubnet = subnetBuilder.build();

        subnetApi.update(getSubnetId(), updateSubnet);
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        NetworkApi networkApi = neutronApi.getNetworkApi(getRegion());

        networkApi.delete(getNetworkId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String networkId = getNetworkId();

        sb.append(getName());
        sb.append(" " );

        if (networkId != null) {
            sb.append(networkId);
        } else {
            sb.append("network");
        }

        return sb.toString();
    }

    public static class Pool {
        private String start;
        private String end;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public AllocationPool allocationPool() {
            return AllocationPool.builder().start(getStart()).end(getEnd()).build();
        }
    }
}