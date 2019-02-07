package beam.openstack.network;

import beam.core.diff.ResourceName;
import beam.lang.Resource;
import beam.openstack.OpenstackResource;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;

import java.io.Closeable;
import java.util.Set;

/**
 * Creates a network.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     openstack::network network-example
 *         network-name: "network-example"
 *     end
 */
@ResourceName("network")
public class NetworkResource extends OpenstackResource {
    private String networkName;
    private String networkId;

    private Boolean adminStateUp;
    private Boolean external;
    private String memberSegments;
    private String multicastIp;
    private String networkFlavor;
    private String networkType;
    private String physicalNetworkName;
    private Boolean portSecurity;
    private String profileId;
    private String segmentAdd;
    private Integer segmentationId;
    private String segmentDel;
    private Boolean shared;
    private String status;
    private String tenantId;

    /**
     * The name of the network. (Required)
     */
    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public Boolean getAdminStateUp() {
        return adminStateUp;
    }

    public void setAdminStateUp(Boolean adminStateUp) {
        this.adminStateUp = adminStateUp;
    }

    public Boolean getExternal() {
        return external;
    }

    public void setExternal(Boolean external) {
        this.external = external;
    }

    public String getMemberSegments() {
        return memberSegments;
    }

    public void setMemberSegments(String memberSegments) {
        this.memberSegments = memberSegments;
    }

    public String getMulticastIp() {
        return multicastIp;
    }

    public void setMulticastIp(String multicastIp) {
        this.multicastIp = multicastIp;
    }

    public String getNetworkFlavor() {
        return networkFlavor;
    }

    public void setNetworkFlavor(String networkFlavor) {
        this.networkFlavor = networkFlavor;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getPhysicalNetworkName() {
        return physicalNetworkName;
    }

    public void setPhysicalNetworkName(String physicalNetworkName) {
        this.physicalNetworkName = physicalNetworkName;
    }

    public Boolean getPortSecurity() {
        return portSecurity;
    }

    public void setPortSecurity(Boolean portSecurity) {
        this.portSecurity = portSecurity;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getSegmentAdd() {
        return segmentAdd;
    }

    public void setSegmentAdd(String segmentAdd) {
        this.segmentAdd = segmentAdd;
    }

    public Integer getSegmentationId() {
        return segmentationId;
    }

    public void setSegmentationId(Integer segmentationId) {
        this.segmentationId = segmentationId;
    }

    public String getSegmentDel() {
        return segmentDel;
    }

    public void setSegmentDel(String segmentDel) {
        this.segmentDel = segmentDel;
    }

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean refresh() {
        NetworkApi client = createClient(NetworkApi.class);

        Network network = client.get(getNetworkId());

        saveNetwork(network);

        return true;
    }

    @Override
    public void create() {
        NetworkApi client = createClient(NetworkApi.class);

        Network network = client.create(Network.CreateNetwork. createBuilder(getNetworkName()).build());

        setNetworkId(network.getId());

        saveNetwork(network);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        NetworkApi client = createClient(NetworkApi.class);

        client.delete(getNetworkId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("network");

        if (!ObjectUtils.isBlank(getNetworkName())) {
            sb.append(" - ").append(getNetworkName());
        }

        return sb.toString();
    }

    @Override
    protected Class<? extends Closeable> getParentClientClass() {
        return NeutronApi.class;
    }

    private void saveNetwork(Network network) {
        setNetworkName(network.getName());
        setAdminStateUp(network.getAdminStateUp());
        setExternal(network.getExternal());
        setMemberSegments(network.getMemberSegments());
        setMulticastIp(network.getMulticastIp());
        setNetworkFlavor(network.getNetworkFlavor());
        setNetworkType(network.getNetworkType().toString());
        setPhysicalNetworkName(network.getPhysicalNetworkName());
        setPortSecurity(network.getPortSecurity());
        setProfileId(network.getProfileId());
        setSegmentAdd(network.getSegmentAdd());
        setSegmentationId(network.getSegmentationId());
        setSegmentDel(network.getSegmentDel());
        setShared(network.getShared());
        setStatus(network.getStatus().toString());
        setTenantId(network.getTenantId());
    }
}
