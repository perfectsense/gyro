package beam.aws.config;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamResource;
import beam.BeamReference;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.Role;

public class InstanceProfileResource extends AWSResource<InstanceProfile> {

    private String instanceProfileName;
    private Set<BeamReference> roles;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass, AWSCredentialsProvider provider) {

        try {
            Constructor<?> constructor = clientClass.getConstructor(AWSCredentialsProvider.class);

            T client = (T) constructor.newInstance(provider);

            if (getRegion() != null) {
                client.setRegion(getRegion());
            }

            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public void setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
    }

    /**
     * @return Never {@code null}.
     */
    @ResourceDiffProperty(updatable = true)
    public Set<BeamReference> getRoles() {
        if (roles == null) {
            roles = new NullSet<>();
        }
        return roles;
    }

    public void setRoles(Set<BeamReference> roles) {
        this.roles = roles;
    }

    @Override
    public String awsId() {
        return getInstanceProfileName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getInstanceProfileName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, InstanceProfile ip) {
        String instanceProfileName = ip.getInstanceProfileName();

        setInstanceProfileName(instanceProfileName);

        // Roles.
        for (Role role : ip.getRoles()) {
            getRoles().add(newReference(RoleResource.class, role.getRoleName()));
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        CreateInstanceProfileRequest crRequest = new CreateInstanceProfileRequest();

        crRequest.setInstanceProfileName(getInstanceProfileName());

        try {
            client.createInstanceProfile(crRequest);
            updateRoles(client);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("EntityAlreadyExists")) {
               throw ase;
            }
        }

        boolean available = false;
        while (!available) {
            ListInstanceProfilesRequest lipRequest = new ListInstanceProfilesRequest();
            ListInstanceProfilesResult lipResult;

            do {
                lipResult = client.listInstanceProfiles(lipRequest);
                for (InstanceProfile instanceProfile : lipResult.getInstanceProfiles()) {
                    if (instanceProfile.getInstanceProfileName().equals(getInstanceProfileName())) {
                        available = true;
                    }
                }

                lipRequest.setMarker(lipResult.getMarker());
            } while (lipResult.isTruncated());

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                return;
            }
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, InstanceProfile> current, Set<String> changedProperties) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());

        updateRoles(client);
    }

    private void updateRoles(AmazonIdentityManagementClient client) {
        AddRoleToInstanceProfileRequest artipRequest = new AddRoleToInstanceProfileRequest();

        artipRequest.setInstanceProfileName(getInstanceProfileName());

        for (BeamReference role : getRoles()) {
            artipRequest.setRoleName(role.awsId());
            client.addRoleToInstanceProfile(artipRequest);
        }
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AWSCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplayString() {
        return "instance profile " + getInstanceProfileName();
    }
}
