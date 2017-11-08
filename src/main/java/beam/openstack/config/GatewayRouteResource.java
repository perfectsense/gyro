package beam.openstack.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GatewayRouteResource extends OpenStackResource<Void> {

    private BeamReference gateway;
    private BeamReference network;

    @ResourceDiffProperty(updatable = true)
    public String getGatewayIp() {
        ServerResource server = (ServerResource) gateway.resolve();
        if (server != null && server.getPort() != null) {
            PortResource port = (PortResource) server.getPort().resolve();
            return port.getIp();
        }

        return null;
    }

    public BeamReference getGateway() {
        return gateway;
    }

    public void setGateway(BeamReference gateway) {
        this.gateway = gateway;
    }

    public BeamReference getNetwork() {
        return network;
    }

    public void setNetwork(BeamReference network) {
        this.network = network;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getGatewayIp());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Void subnet) {

    }

    @Override
    public void create(OpenStackCloud cloud) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        SubnetApi subnetApi = neutronApi.getSubnetApi(getRegion());

        NetworkResource networkResource = (NetworkResource) network.resolve();
        ServerResource server = (ServerResource) gateway.resolve();

        Subnet.UpdateBuilder subnetBuilder = Subnet.UpdateSubnet.updateBuilder()
                .gatewayIp(server.getPrivateIP())
                .name(networkResource.getName());

        Subnet.UpdateSubnet updateSubnet = subnetBuilder.build();

        subnetApi.update(networkResource.getSubnetId(), updateSubnet);
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Void> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("gateway route ");

        if (getGatewayIp() != null) {
            sb.append("to ");
            sb.append(getGatewayIp());
        }

        return sb.toString();
    }

}