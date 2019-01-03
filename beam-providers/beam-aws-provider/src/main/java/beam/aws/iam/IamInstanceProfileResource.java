package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileResponse;
import software.amazon.awssdk.services.iam.model.Role;

import java.util.HashSet;
import java.util.Set;


@ResourceName("instance-profile")
public class IamInstanceProfileResource extends AwsResource {

    private String instanceProfileName;
    private String roleName;
    private Set<IamRoleResource> roles;

    public String getinstanceProfileName() {
        return this.instanceProfileName;
    }

    public void setinstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<IamRoleResource> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<IamRoleResource> roles) {
        this.roles = roles;
    }


    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        GetInstanceProfileResponse response = client.getInstanceProfile(r -> r.instanceProfileName(getinstanceProfileName()));

        if (response != null) {
            Set<IamRoleResource> roles = new HashSet<>();
            for (Role role : response.instanceProfile().roles()) {
                IamRoleResource newRole = new IamRoleResource();
                newRole.setAssumeRolePolicyDocument(role.assumeRolePolicyDocument());
                newRole.setDescription(role.description());
                newRole.setRoleName(role.roleName());
                roles.add(newRole);
            }
            setRoles(roles);
            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.createInstanceProfile(r -> r.instanceProfileName(getRoleName()));

        for (IamRoleResource role : getRoles()) {
            client.addRoleToInstanceProfile(
                r -> r.roleName(role.getRoleName())
                .instanceProfileName(getinstanceProfileName()));
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {}

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.deleteInstanceProfile(r -> r.instanceProfileName(getRoleName()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getinstanceProfileName() != null) {
            sb.append("instance profile " + getinstanceProfileName());

        } else {
            sb.append("instance profile ");
        }

        return sb.toString();
    }


}