package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileResponse;
import software.amazon.awssdk.services.iam.model.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a Instance Profile.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::iam-instance-profile ex-inst-profile
 *         instance-profile-name: "ex-inst-profile"
 *         role-name: $(aws::role-resource example-role | role-name)
 *         roles: [$(aws::role-resource example-role | role-name)]
 *
 *     end
 *
 *     aws::iam-role example-role
 *         role-name: "example-role"
 *         description: "description"
 *         assume-role-policy-document-file: "role_example.json"
 *
 *     end
 */

@ResourceName("iam-instance-profile")
public class IamInstanceProfileResource extends AwsResource {

    private String instanceProfileName;
    private String roleName;
    private List<String> roles;

    public String getInstanceProfileName() {
        return this.instanceProfileName;
    }

    public void setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<>();
        }
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }


    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        GetInstanceProfileResponse response = client.getInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName()));
        System.out.println("Am I even in here ");
        if (response != null) {
            getRoles().clear();
            for (Role role : response.instanceProfile().roles()) {
                getRoles().add(role.roleName());
            }
            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.createInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName()));

        System.out.println("What is the instance profile name "+getInstanceProfileName());
        System.out.println("What is role name "+getRoleName());
        System.out.println("What are roles "+getRoles());

        for (String role : getRoles()) {
            client.addRoleToInstanceProfile(
                r -> r.roleName(role)
                .instanceProfileName(getInstanceProfileName()));
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

        if (getInstanceProfileName() != null) {
            sb.append("instance profile " + getInstanceProfileName());

        } else {
            sb.append("instance profile ");
        }

        return sb.toString();
    }
}