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
    public String getAssumeRolePolicyContents() {
        /*
        String contents = "";
        try {
            contents = new String(Files.readAllBytes(Paths.get(getAssumeRolePolicyDocumentFile())), "UTF-8");
        } catch(Exception err){
            throw new BeamException(err.getMessage());
        }
        return contents;
        */
        return getDocument();
    }

    public void setAssumeRolePolicyContents(String assumeRolePolicyContents) {
        this.assumeRolePolicyContents = assumeRolePolicyContents;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyDocumentFile() {
        return this.assumeRolePolicyDocumentFile;
    }

    public void setAssumeRolePolicyDocumentFile(String assumeRolePolicyDocumentFile) {
        this.assumeRolePolicyDocumentFile = assumeRolePolicyDocumentFile;
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
            setAssumeRolePolicyContents(response.assumeRolePolicyDocument());

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
        //System.out.println("What is the policy document as a whole"+ ObjectUtils.toJson(this.getAssumeRolePolicyContents()));
        /*
        String contents = "";
        try {
            contents = new String(Files.readAllBytes(Paths.get(this.getAssumeRolePolicyDocumentFile())), "UTF-8");
        } catch(Exception err){

        }
        System.out.println("\nwhat are the policy contents "+contents);

        setAssumeRolePolicyContents(contents);
        */
        //System.out.println("\nwhat are the policy contents "+getAssumeRolePolicyContents());
        //System.out.println("\nwhat are the policy contents "+this.getAssumeRolePolicyContents());


        CreateRoleRequest request = CreateRoleRequest.builder()
                .assumeRolePolicyDocument(getAssumeRolePolicyContents())
                .description(getDescription())
                .roleName(getRoleName())
                .build();

        client.createRole(request);



        //System.out.println("What is stored in content "+contents);

        //System.out.println("MADE IT HERE IN CREATE ROLE");
        //System.out.println("Get policies "+getPolicies());
        //System.out.println("Get policies SIZE "+getPolicies().size());

        for(String policyArn: getPolicyArns()){
            //System.out.println("POLICY ARN OF POLICY TO ATTACH IS "+policyArn);
            client.attachRolePolicy(r -> r.roleName(getRoleName())
                    .policyArn(policyArn));
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        System.out.println("Changed properties are: "+changedProperties);
        IamRoleResource currentResource = (IamRoleResource) current;
        System.out.println("policy contents "+((IamRoleResource) current).getAssumeRolePolicyContents());
        System.out.println("get new policy contents "+getAssumeRolePolicyContents());
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        UpdateAssumeRolePolicyRequest updatePolicyRequest = UpdateAssumeRolePolicyRequest.builder()
                .policyDocument(getAssumeRolePolicyContents())
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

    public String getDocument(){

        String contents = "";
        try {
            contents = new String(Files.readAllBytes(Paths.get(getAssumeRolePolicyDocumentFile())), "UTF-8");
        } catch(Exception err){
            throw new BeamException(err.getMessage());
        }
        return contents;
    }

}