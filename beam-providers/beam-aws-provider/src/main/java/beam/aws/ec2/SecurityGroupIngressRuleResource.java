package beam.aws.ec2;

import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IpPermission;

import java.util.Set;

@ResourceName(parent = "security-group", value = "ingress")
public class SecurityGroupIngressRuleResource extends SecurityGroupRuleResource {

    public SecurityGroupIngressRuleResource() {
    }

    public SecurityGroupIngressRuleResource(IpPermission permission) {
        super(permission);
    }

    @Override
    public String resourceType() {
        return "ingress";
    }

    @Override
    public void create() {
        Ec2Client client = createClient(Ec2Client.class);
        client.authorizeSecurityGroupIngress(r -> r.groupId(getGroupId()).ipPermissions(getIpPermissionRequest()));
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        if (changedProperties.size() == 1 && changedProperties.contains("description")) {
            client.updateSecurityGroupRuleDescriptionsIngress(r -> r.groupId(getGroupId()).ipPermissions(getIpPermissionRequest()));
        } else {
            current.delete();
            create();
        }

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        try {
            client.revokeSecurityGroupIngress(r -> r.groupId(getGroupId()).ipPermissions(getIpPermissionRequest()));
        } catch (Ec2Exception eex) {
            if (!eex.awsErrorDetails().errorCode().equals("InvalidPermission.NotFound")) {
                throw eex;
            }
        }
    }

}
