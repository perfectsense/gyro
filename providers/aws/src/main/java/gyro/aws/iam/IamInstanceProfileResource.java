package gyro.aws.iam;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateInstanceProfileResponse;
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
 * .. code-block:: gyro
 *
 *     aws::iam-instance-profile ex-inst-profile
 *         instance-profile-name: "ex-inst-profile"
 *     end
 */

@ResourceName("iam-instance-profile")
public class IamInstanceProfileResource extends AwsResource {

    private String instanceProfileArn;
    private String instanceProfileName;
    private List<String> roles;

    public String getInstanceProfileArn() {
        return instanceProfileArn;
    }

    public void setInstanceProfileArn(String instanceProfileArn) {
        this.instanceProfileArn = instanceProfileArn;
    }

    public String getInstanceProfileName() {
        return this.instanceProfileName;
    }

    public void setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
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

        if (response != null) {
            setInstanceProfileArn(response.instanceProfile().arn());

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

        try {
            CreateInstanceProfileResponse response = client.createInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName()));

            setInstanceProfileArn(response.instanceProfile().arn());

            for (String role : getRoles()) {
                client.addRoleToInstanceProfile(
                    r -> r.roleName(role)
                            .instanceProfileName(getInstanceProfileName()));
            }
        } catch (Exception err) {
            delete();
            throw new BeamException(err.getMessage());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        IamInstanceProfileResource currentResource = (IamInstanceProfileResource) current;

        List<String> additions = new ArrayList<>(getRoles());
        additions.removeAll(currentResource.getRoles());

        List<String> subtractions = new ArrayList<>(currentResource.getRoles());
        subtractions.removeAll(getRoles());

        for (String addRole : additions) {
            client.addRoleToInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName())
                    .roleName(addRole));
        }

        for (String deleteRole : subtractions) {
            client.removeRoleFromInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName())
                    .roleName(deleteRole));
        }
    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        GetInstanceProfileResponse response = client.getInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName()));
        for (Role removeRole: response.instanceProfile().roles()) {
            client.removeRoleFromInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName())
                                                    .roleName(removeRole.roleName()));
        }

        client.deleteInstanceProfile(r -> r.instanceProfileName(getInstanceProfileName()));
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