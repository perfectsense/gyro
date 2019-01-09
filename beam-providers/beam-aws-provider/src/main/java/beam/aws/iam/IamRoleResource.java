package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.net.URLDecoder;

/**
 * Creates a Role Resource with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::role-resource example-role
 *         description: testing the role functionality
 *         assume-role-policy-document-file: role_example
 *         role-name: rta-test-role
 *
 *     end
 */

@ResourceName("role-resource")
public class IamRoleResource extends AwsResource {

    private String roleName;
    private String description;
    private String assumeRolePolicyContents;
    private String assumeRolePolicyDocumentFile;
    private List<String> policyArns;

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyDocumentFile() {
        return this.assumeRolePolicyDocumentFile;
    }

    public void setAssumeRolePolicyDocumentFile(String assumeRolePolicyDocumentFile) {
        this.assumeRolePolicyDocumentFile = assumeRolePolicyDocumentFile;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyContents() {

        if(assumeRolePolicyContents != null){
            return assumeRolePolicyContents;
        }
        else {
            if(getAssumeRolePolicyDocumentFile() != null) {
                try {
                    return new String(Files.readAllBytes(Paths.get(getAssumeRolePolicyDocumentFile())), "UTF-8");
                } catch (Exception err) {
                    throw new BeamException(err.getMessage());
                }
            } else {
                return null;
            }
        }
    }

    public void setAssumeRolePolicyContents(String assumeRolePolicyContents) {
        this.assumeRolePolicyContents = assumeRolePolicyContents;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getPolicyArns() {
        if (this.policyArns == null) {
            this.policyArns = new ArrayList<>();
        }

        return this.policyArns;
    }

    public void setPolicyArns(List<String> policyArns) {
        this.policyArns = policyArns;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        Role response = client.getRole(r -> r.roleName(getRoleName())).role();
        if (response != null) {
            setRoleName(response.roleName());
            setDescription(response.description());
            String encode = URLDecoder.decode(response.assumeRolePolicyDocument());
            System.out.println("Show encoded "+encode);
            setAssumeRolePolicyContents(URLDecoder.decode(response.assumeRolePolicyDocument()));
            System.out.println("Past contents "+response.assumeRolePolicyDocument());

            ListAttachedRolePoliciesResponse policyResponse = client.listAttachedRolePolicies(r -> r.roleName(getRoleName()));
            for (AttachedPolicy attachedPolicy: policyResponse.attachedPolicies()) {
                getPolicyArns().add(attachedPolicy.policyArn());
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

        System.out.println("What is role policy document filename"+getAssumeRolePolicyDocumentFile());

        client.createRole(r -> r.assumeRolePolicyDocument(getAssumeRolePolicyContents())
                                .description(getDescription())
                                .roleName(getRoleName()));

        for(String policyArn: getPolicyArns()){
            client.attachRolePolicy(r -> r.roleName(getRoleName())
                    .policyArn(policyArn));
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        System.out.println("Changed properties are: "+changedProperties);
        IamRoleResource currentResource = (IamRoleResource) current;
        System.out.println("policy contents "+currentResource.getAssumeRolePolicyContents());
        System.out.println("get new policy contents "+getAssumeRolePolicyContents());
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.updateAssumeRolePolicy(r -> r.policyDocument(getAssumeRolePolicyContents())
                                            .roleName(getRoleName()));

        client.updateRole(r -> r.description(getDescription())
                                .roleName(getRoleName()));

    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        ListAttachedRolePoliciesResponse response = client.listAttachedRolePolicies(r -> r.roleName(getRoleName()));
        for(AttachedPolicy policies : response.attachedPolicies()){
            client.detachRolePolicy(r -> r.policyArn(policies.policyArn())
                                        .roleName(getRoleName()));
        }

        client.deleteRole(r -> r.roleName(getRoleName()));

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getRoleName() != null) {
            sb.append("role name " + getRoleName());

        } else {
            sb.append("role name ");
        }

        return sb.toString();
    }

}