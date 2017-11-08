package beam.aws.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

public class SecurityGroupResource extends TaggableEC2Resource<SecurityGroup> {

    private String description;
    private String groupId;
    private String groupName;
    private Set<SecurityGroupIpPermissionResource> ipPermissions;
    private BeamReference vpc;

    @ResourceDiffProperty
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Set<SecurityGroupIpPermissionResource> getIpPermissions() {
        if (ipPermissions == null) {
            ipPermissions = new HashSet<>();
        }
        return ipPermissions;
    }

    public void setIpPermissions(Set<SecurityGroupIpPermissionResource> ipPermissions) {
        this.ipPermissions = ipPermissions;
    }

    public BeamReference getVpc() {
        return newParentReference(VpcResource.class, vpc);
    }

    public void setVpc(BeamReference vpc) {
        this.vpc = vpc;
    }

    @Override
    public String awsId() {
        return getGroupId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getGroupId(), getGroupName());
    }

    @Override
    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, SecurityGroup group) {
        setDescription(group.getDescription());
        setGroupId(group.getGroupId());
        setGroupName(group.getGroupName());

        for (IpPermission perm : group.getIpPermissions()) {
            if (!isInclude(filter, perm)) {
                continue;
            }

            List<String> ipRanges = perm.getIpRanges();

            for (UserIdGroupPair pair : perm.getUserIdGroupPairs()) {
                SecurityGroupIpPermissionResource permResource = new SecurityGroupIpPermissionResource();
                permResource.setRegion(getRegion());

                permResource.init(cloud, filter, perm);
                permResource.setFromGroup(newReference(SecurityGroupResource.class, pair.getGroupId()));

                getIpPermissions().add(permResource);
            }

            for (String ipRange : ipRanges) {
                SecurityGroupIpPermissionResource permResource = new SecurityGroupIpPermissionResource();
                permResource.setRegion(getRegion());

                permResource.init(cloud, filter, perm);
                permResource.setIpRange(ipRange);
                getIpPermissions().add(permResource);
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getIpPermissions());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, SecurityGroup> current) throws Exception {
        SecurityGroupResource currentSg = (SecurityGroupResource) current;

        update.update(currentSg.getIpPermissions(), getIpPermissions());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getIpPermissions());
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateSecurityGroupRequest csgRequest = new CreateSecurityGroupRequest();

        csgRequest.setDescription(getDescription());
        csgRequest.setGroupName(getGroupName());
        csgRequest.setVpcId(getVpc().awsId());
        setGroupId(client.createSecurityGroup(csgRequest).getGroupId());
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<SecurityGroup> current, Set<String> changedProperties) {
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteSecurityGroupRequest dsgRequest = new DeleteSecurityGroupRequest();

        dsgRequest.setGroupId(getGroupId());
        client.deleteSecurityGroup(dsgRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String groupId = getGroupId();

        if (groupId != null) {
            sb.append(groupId);

        } else {
            sb.append("security group");
        }

        String groupName = getGroupName();

        if (groupName != null) {
            sb.append(' ');
            sb.append(groupName);
        }

        return sb.toString();
    }
}
