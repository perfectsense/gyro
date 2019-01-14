package beam.test;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import java.util.UUID;

@ResourceName("network")
public class NetworkResource extends FakeResource {

    private String networkId;
    private String cidrBlock;
    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;

    // Read-only
    private String ownerId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
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
    public void create() {
        setNetworkId("network-" + UUID.randomUUID().toString().replace("-", "").substring(16));
        setOwnerId("owner-4a4f49a0b9fe");
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String networkId = getNetworkId();

        if (networkId != null) {
            sb.append(networkId);

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
