package beam.test;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import java.util.UUID;

@ResourceName("subnet")
public class SubnetResource extends FakeResource {

    private String networkId;
    private String cidrBlock;
    private String availabilityZone;
    private Boolean mapPublicIpOnLaunch;
    private String subnetId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * The IPv4 network range for the subnet, in CIDR notation. (Required)
     */
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    /**
     * The name of the availablity zone to create this subnet (ex. ``us-east-1a``).
     */
    @ResourceDiffProperty
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     * Assign a public IPv4 address to network interfaces created in this subnet.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(Boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    public void create() {
        setSubnetId("subnet-" + UUID.randomUUID().toString().replace("-", "").substring(16));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String subnetId = getSubnetId();

        if (subnetId != null) {
            sb.append(subnetId);

        } else {
            sb.append("fake subnet");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(getCidrBlock());
        }

        String availabilityZone = getAvailabilityZone();

        if (availabilityZone != null) {
            sb.append(" in ");
            sb.append(availabilityZone);
        }

        return sb.toString();
    }
}
