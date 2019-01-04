package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ResourceName("security-group")
public class SecurityGroupResource extends Ec2TaggableResource<SecurityGroup> {

    private String groupName;
    private String vpcId;
    private String description;
    private List<SecurityGroupIngressRuleResource> ingress;
    private List<SecurityGroupEgressRuleResource> egress;
    private Boolean keepDefaultEgressRules;
    private String groupId;
    private String ownerId;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ResourceDiffProperty(nullable = true, delegate = true, subresource = true)
    public List<SecurityGroupIngressRuleResource> getIngress() {
        if (ingress == null) {
            ingress = new ArrayList<>();
        }

        return ingress;
    }

    public void setIngress(List<SecurityGroupIngressRuleResource> ingress) {
        this.ingress = ingress;
    }

    @ResourceDiffProperty(nullable = true, delegate = true, subresource = true)
    public List<SecurityGroupEgressRuleResource> getEgress() {
        if (egress == null) {
            egress = new ArrayList<>();
        }

        return egress;
    }

    public void setEgress(List<SecurityGroupEgressRuleResource> egress) {
        this.egress = egress;
    }

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

    @Override
    protected String getId() {
        return getGroupId();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    protected boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        Filter nameFilter = Filter.builder().name("group-name").values(getGroupName()).build();
        Filter vpcFilter = Filter.builder().name("vpc-id").values(getVpcId()).build();

        DescribeSecurityGroupsResponse response = client.describeSecurityGroups(
            r -> r.filters(nameFilter, vpcFilter)
        );

        for (SecurityGroup group : response.securityGroups()) {
            setOwnerId(group.ownerId());
            setVpcId(group.vpcId());
            setDescription(group.description());

            getEgress().clear();
            for (IpPermission permission : group.ipPermissionsEgress()) {
                if (isKeepDefaultEgressRules() && permission.ipProtocol().equals("-1")
                    && permission.fromPort() == null && permission.toPort() == null
                    && permission.ipRanges().get(0).cidrIp().equals("0.0.0.0/0")) {
                    continue;
                }

                SecurityGroupEgressRuleResource rule = new SecurityGroupEgressRuleResource(permission);
                rule.setParentNode(this);
                rule.setResourceCredentials(getResourceCredentials());
                getEgress().add(rule);
            }

            getIngress().clear();
            for (IpPermission permission : group.ipPermissions()) {
                SecurityGroupIngressRuleResource rule = new SecurityGroupIngressRuleResource(permission);
                rule.setParentNode(this);
                rule.setResourceCredentials(getResourceCredentials());
                getIngress().add(rule);
            }

            return true;
        }

        return false;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateSecurityGroupResponse response = client.createSecurityGroup(
            r -> r.vpcId(getVpcId()).description(getDescription()).groupName(getGroupName())
        );

        setGroupId(response.groupId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteSecurityGroup(r -> r.groupId(getGroupId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String groupId = getGroupId();

        sb.append("security group");
        if (groupId != null) {
            sb.append(" ").append(groupId);
        }

        String groupName = getGroupName();

        if (groupName != null) {
            sb.append(' ');
            sb.append(groupName);
        }

        return sb.toString();
    }

}

