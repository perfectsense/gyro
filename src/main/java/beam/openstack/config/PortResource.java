package beam.openstack.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.ImmutableSet;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.features.PortApi;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PortResource extends OpenStackResource<Port> {

    private String portId;
    private String deviceId;
    private String name;
    private String ip;
    private BeamReference network;

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = portId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BeamReference getNetwork() {
        return network;
    }

    public void setNetwork(BeamReference network) {
        this.network = network;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getNetwork(), getIp());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Port port) {
        setName(port.getName());
        setPortId(port.getId());
        setDeviceId(port.getDeviceId());
        for (IP portIp : port.getFixedIps()) {
            setIp(portIp.getIpAddress());
            break;
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        PortApi portApi = neutronApi.getPortApi(getRegion());

        NetworkResource networkResource = (NetworkResource) network.resolve();

        IP ip = IP.builder().ipAddress(getIp()).subnetId(networkResource.getSubnetId()).build();
        Port.CreatePort port = Port.createBuilder(networkResource.getNetworkId())
                .name(getName())
                .fixedIps(ImmutableSet.of(ip)).build();

        Port create = portApi.create(port);

        setPortId(create.getId());
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Port> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        return "port with fixed ip " + getIp();
    }

}