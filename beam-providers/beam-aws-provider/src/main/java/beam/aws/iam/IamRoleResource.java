package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 *         assumeRolePolicyDocumentFile: role_example
 *         roleName: rta-test-role
 *
 *     end
 */

@ResourceName("role-resource")
public class IamRoleResource extends AwsResource {

    private String roleName;
    private String description;
    private Map<String, Object> assumeRolePolicyContents;
    private String assumeRolePolicyDocument;
    private File assumeRolePolicyDocumentFile;
    private Set<String> policies;

    private void setAssumeRolePolicyContents(Map<String, Object> assumeRolePolicyContents) {
        this.assumeRolePolicyContents = assumeRolePolicyContents;
    }

    public String getAssumeRolePolicyDocument() {
        return this.assumeRolePolicyDocument;
    }

    public void setAssumeRolePolicyDocument(String assumeRolePolicyDocument) {
        this.assumeRolePolicyDocument = assumeRolePolicyDocument;
    }

    @ResourceDiffProperty(updatable = true)
    public File getAssumeRolePolicyDocumentFile() {
        return this.assumeRolePolicyDocumentFile;
    }

    public void setAssumeRolePolicyDocumentFile(File assumeRolePolicyDocumentFile) {
        this.assumeRolePolicyDocumentFile = assumeRolePolicyDocumentFile;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPolicies() {
        if (this.policies == null) {
            this.policies = new HashSet();
        }

        return this.policies;
    }

    public void setPolicies(Set<String> policies) {
        this.policies = policies;
    }

    @ResourceDiffProperty(updatable = true)
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
            setAssumeRolePolicyContents((Map) ObjectUtils.fromJson(StringUtils.decodeUri(response.assumeRolePolicyDocument())));
            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();
        /*
        try {
            preparePolicy();
        } catch (Exception err) {
            err.getMessage();
        }
        */
        //System.out.println("\nwhat is the role name "+getRoleName());
        //System.out.println("\nwhat is the description "+getDescription());
        //System.out.println("What is role policy document name"+getAssumeRolePolicyDocumentFile());
        //System.out.println("What is the policy document as a whole"+ ObjectUtils.toJson(this.getAssumeRolePolicyContents()));

        CreateRoleRequest request = CreateRoleRequest.builder()
                .assumeRolePolicyDocument(ObjectUtils.toJson(this.getAssumeRolePolicyContents()))
                .description(getDescription())
                .roleName(getRoleName())
                .build();

        client.createRole(request);


        //System.out.println("MADE IT HERE IN CREATE ROLE");
        //System.out.println("Get policies "+getPolicies());
        for(String policyArn: getPolicies()){
            System.out.println("POLICY ARN OF POLICY TO ATTACH IS "+policyArn);
            client.attachRolePolicy(r -> r.roleName(getRoleName())
                    .policyArn(policyArn));
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        UpdateAssumeRolePolicyRequest updatePolicyRequest = UpdateAssumeRolePolicyRequest.builder()
                .policyDocument(ObjectUtils.toJson(this.getAssumeRolePolicyDocument()))
                .roleName(getRoleName())
                .build();

        client.updateAssumeRolePolicy(updatePolicyRequest);

        UpdateRoleRequest updateRoleRequest = UpdateRoleRequest.builder()
                .description(getDescription())
                .roleName(getRoleName())
                .build();

        client.updateRole(updateRoleRequest);

    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        DeleteRoleRequest request = DeleteRoleRequest.builder()
                .roleName(getRoleName())
                .build();

        client.deleteRole(request);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String roleName = getRoleName();

        if (roleName != null) {
            sb.append("role name " + roleName);

        } else {
            sb.append("role name ");
        }

        return sb.toString();
    }

}