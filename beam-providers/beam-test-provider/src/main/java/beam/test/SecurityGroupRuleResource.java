package beam.test;

import beam.core.diff.ResourceDiffProperty;

import java.util.ArrayList;
import java.util.List;

public abstract class SecurityGroupRuleResource extends FakeResource {

    private List<String> cidrBlocks;
    private String protocol;
    private String description;
    private Integer fromPort;
    private Integer toPort;

    public String getGroupId() {
        SecurityGroupResource parent = (SecurityGroupResource) parent();
        if (parent != null) {
            return parent.getGroupId();
        }

        return null;
    }

    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        if (protocol != null) {
            return protocol.toLowerCase();
        }

        return "tcp";
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getFromPort() {
        return fromPort;
    }

    public void setFromPort(Integer fromPort) {
        this.fromPort = fromPort;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getToPort() {
        return toPort;
    }

    public void setToPort(Integer toPort) {
        this.toPort = toPort;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getCidrBlocks() {
        if (cidrBlocks == null) {
            cidrBlocks = new ArrayList<>();
        }

        return cidrBlocks;
    }

    public void setCidrBlocks(List<String> cidrBlocks) {
        this.cidrBlocks = cidrBlocks;
    }

    @Override
    public String primaryKey() {
        return String.format("%s %d %d", getProtocol(), getFromPort(), getToPort());
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(resourceType());
        sb.append(" fake security rule - ");
        sb.append(getProtocol());
        sb.append(" [");
        sb.append(getFromPort());
        sb.append(" to ");
        sb.append(getToPort());
        sb.append("]");

        if (!getCidrBlocks().isEmpty()) {
            sb.append(" ");
            sb.append(getCidrBlocks());
        }

        sb.append(" ");
        sb.append(getDescription());

        return sb.toString();
    }

}

