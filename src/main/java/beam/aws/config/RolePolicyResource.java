package beam.aws.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;

public class RolePolicyResource extends AWSResource<Void> {

    private BeamReference role;
    private String policyName;
    private Map<String, Object> policyDocument;
    private File policyDocumentFile;

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

    public BeamReference getRole() {
        return newParentReference(RoleResource.class, role);
    }

    public void setRole(BeamReference role) {
        this.role = role;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    @ResourceDiffProperty(updatable = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPolicyDocument() {
        if (policyDocument != null) {
            return policyDocument;

        } else {
            File file = getPolicyDocumentFile();

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

    public void setPolicyDocument(Map<String, Object> policyDocument) {
        this.policyDocument = policyDocument;
    }

    public File getPolicyDocumentFile() {
        return policyDocumentFile;
    }

    public void setPolicyDocumentFile(File policyDocumentFile) {
        this.policyDocumentFile = policyDocumentFile;
    }

    @Override
    public List<Object> diffIds() {
        return Arrays.asList(getRole(), getPolicyName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, Void v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        PutRolePolicyRequest prpRequest = new PutRolePolicyRequest();

        prpRequest.setPolicyDocument(ObjectUtils.toJson(getPolicyDocument()));
        prpRequest.setPolicyName(getPolicyName());
        prpRequest.setRoleName(getRole().awsId());
        client.putRolePolicy(prpRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Void> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonIdentityManagementClient client = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());
        DeleteRolePolicyRequest drpRequest = new DeleteRolePolicyRequest();

        drpRequest.setRoleName(getRole().awsId());
        drpRequest.setPolicyName(getPolicyName());
        client.deleteRolePolicy(drpRequest);
    }

    @Override
    public String toDisplayString() {
        return "policy " + getPolicyName();
    }
}
