package beam.test;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ResourceName("security-group")
public class SecurityGroupResource extends FakeResource {

    private String groupName;
    private String networkId;
    private String description;
    private List<SecurityGroupIngressRuleResource> ingress;
    private List<SecurityGroupEgressRuleResource> egress;
    private Boolean keepDefaultEgressRules;
    private String groupId;
    private String ownerId;

    /**
     * The name of the security group.
     */
    @ResourceDiffProperty
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * The ID of the network to associate this security group with.
     */
    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * The description of this security group.
     */
    @ResourceDiffProperty
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * A list of ingress rules to block inbound traffic.
     */
    @ResourceDiffProperty(nullable = true, subresource = true)
    public List<SecurityGroupIngressRuleResource> getIngress() {
        if (ingress == null) {
            ingress = new ArrayList<>();
        }

        return ingress;
    }

    public void setIngress(List<SecurityGroupIngressRuleResource> ingress) {
        this.ingress = ingress;
    }

    /**
     * A list of egress rules to block outbound traffic.
     */
    @ResourceDiffProperty(nullable = true, subresource = true)
    public List<SecurityGroupEgressRuleResource> getEgress() {
        if (egress == null) {
            egress = new ArrayList<>();
        }

        return egress;
    }

    public void setEgress(List<SecurityGroupEgressRuleResource> egress) {
        this.egress = egress;
    }

    /**
     * Whether to keep the default egress rule. If false, the rule will be deleted.
     */
    @ResourceDiffProperty(updatable = true)
    public boolean isKeepDefaultEgressRules() {
        if (keepDefaultEgressRules == null) {
            keepDefaultEgressRules = true;
        }

        return keepDefaultEgressRules;
    }

    public void setKeepDefaultEgressRules(boolean keepDefaultEgressRules) {
        this.keepDefaultEgressRules = keepDefaultEgressRules;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public void create() {
        setGroupId("sg-" + UUID.randomUUID().toString().replace("-", "").substring(16));
        setOwnerId("owner-4a4f49a0b9fe");
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String groupId = getGroupId();

        sb.append("fake security group");
        if (groupId != null) {
            sb.append(" ").append(groupId);
        }

        String groupName = getGroupName();

        if (groupName != null) {
            sb.append(" - ");
            sb.append(groupName);
        }

        return sb.toString();
    }

}

