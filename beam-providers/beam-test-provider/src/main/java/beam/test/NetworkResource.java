package beam.test;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

@ResourceName("network")
public class NetworkResource extends FakeResource {

    private String vpcId;
    private String cidrBlock;
    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;
    private String instanceTenancy;

    // Read-only
    private String ownerId;

    public String getId() {
        return getVpcId();
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @ResourceDiffProperty
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsHostnames() {
        if (enableDnsHostnames == null) {
            enableDnsHostnames = true;
        }

        return enableDnsHostnames;
    }

    public void setEnableDnsHostnames(Boolean enableDnsHostnames) {
        this.enableDnsHostnames = enableDnsHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsSupport() {
        if (enableDnsSupport == null) {
            enableDnsSupport = true;
        }

        return enableDnsSupport;
    }

    public void setEnableDnsSupport(Boolean enableDnsSupport) {
        this.enableDnsSupport = enableDnsSupport;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String vpcId = getVpcId();

        if (vpcId != null) {
            sb.append(vpcId);

        } else {
            sb.append("fake network");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(cidrBlock);
        }

        sb.append(" - ");
        sb.append(resourceIdentifier());

        return sb.toString();
    }

}
