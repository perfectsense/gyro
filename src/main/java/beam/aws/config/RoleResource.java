package beam.aws.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.UpdateAssumeRolePolicyRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public class RoleResource extends AWSResource<Role> {

    private Map<String, Object> assumeRolePolicyDocument;
    private File assumeRolePolicyDocumentFile;
    private String roleName;
    private Set<RolePolicyResource> policies;

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

    @ResourceDiffProperty(updatable = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAssumeRolePolicyDocument() {
        if (assumeRolePolicyDocument != null) {
            return assumeRolePolicyDocument;

        } else {
            File file = getAssumeRolePolicyDocumentFile();

            if (file != null) {
                try {
                    return (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(file, Charsets.UTF_8));

                } catch (IOException error) {
                    throw Throwables.propagate(error);
                }

            } else {
                return null;
            }
        }
    }

    public void setAssumeRolePolicyDocument(Map<String, Object> assumeRolePolicyDocument) {
        this.assumeRolePolicyDocument = assumeRolePolicyDocument;
    }

    public File getAssumeRolePolicyDocumentFile() {
        return assumeRolePolicyDocumentFile;
    }

    public void setAssumeRolePolicyDocumentFile(File assumeRolePolicyDocumentFile) {
        this.assumeRolePolicyDocumentFile = assumeRolePolicyDocumentFile;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<RolePolicyResource> getPolicies() {
        if (policies == null) {
            policies = new HashSet<>();
        }
        return policies;
    }

    public void setPolicies(Set<RolePolicyResource> policies) {
        this.policies = policies;
    }

    @Override
    public String awsId() {
        return getRoleName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getRoleName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(AWSCloud cloud, BeamResourceFilter filter, Role role) {
        String roleName = role.getRoleName();

        setAssumeRolePolicyDocument((Map<String, Object>) ObjectUtils.fromJson(StringUtils.decodeUri(role.getAssumeRolePolicyDocument())));
        setRoleName(roleName);

        // Policies.
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        ListRolePoliciesRequest lrpRequest = new ListRolePoliciesRequest();

        lrpRequest.setRoleName(getRoleName());

        for (String policyName : client.
                listRolePolicies(lrpRequest).
                getPolicyNames()) {

            RolePolicyResource policyResource = new RolePolicyResource();
            policyResource.setRegion(getRegion());

            getPolicies().add(policyResource);
            policyResource.setPolicyName(policyName);

            GetRolePolicyRequest grpRequest = new GetRolePolicyRequest();

            grpRequest.setRoleName(roleName);
            grpRequest.setPolicyName(policyName);
            policyResource.setPolicyDocument((Map<String, Object>) ObjectUtils.fromJson(StringUtils.decodeUri(client.getRolePolicy(grpRequest).getPolicyDocument())));
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getPolicies());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Role> current) throws Exception {
        RoleResource currentRole = (RoleResource) current;

        update.update(currentRole.getPolicies(), getPolicies());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getPolicies());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        CreateRoleRequest crRequest = new CreateRoleRequest();

        crRequest.setAssumeRolePolicyDocument(ObjectUtils.toJson(getAssumeRolePolicyDocument()));
        crRequest.setRoleName(getRoleName());

        try {
            client.createRole(crRequest);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("EntityAlreadyExists")) {
                throw ase;
            }
        }

        boolean available = false;
        while (!available) {
            ListRolesRequest lrpRequest = new ListRolesRequest();
            ListRolesResult lrResult;

            do {
                lrResult = client.listRoles(lrpRequest);
                for (Role role : lrResult.getRoles()) {
                    if (role.getRoleName().equals(getRoleName())) {
                        available = true;
                    }
                }

                lrpRequest.setMarker(lrResult.getMarker());
            } while (lrResult.isTruncated());

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                return;
            }
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Role> current, Set<String> changedProperties) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        UpdateAssumeRolePolicyRequest uarpRequest = new UpdateAssumeRolePolicyRequest();

        uarpRequest.setPolicyDocument(ObjectUtils.toJson(getAssumeRolePolicyDocument()));
        uarpRequest.setRoleName(getRoleName());
        client.updateAssumeRolePolicy(uarpRequest);
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
        return "role " + getRoleName();
    }
}
