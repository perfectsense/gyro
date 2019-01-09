package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iam.model.GetPolicyResponse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Creates a Policy with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::iam-policy example-role
 *         description: testing the policy functionality
 *         policy-name: rta-test-policy
 *         policy-document-file: policyFile.json
 *
 *     end
 */

@ResourceName("iam-policy")
public class IamPolicyResource extends AwsResource {
    private String policyName;
    private String description;
    private String policyArn;
    private String policyDocumentContents;
    private String policyDocumentFile;

    @ResourceDiffProperty(updatable = true)
    public String getPolicyName() {
        return this.policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyArn() {
        return this.policyArn;
    }

    public void setPolicyArn(String policyArn) {
        this.policyArn = policyArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPolicyDocumentContents() {
        if(policyDocumentContents != null){
            return policyDocumentContents;
        }
        else {
            if(getPolicyDocumentFile() != null) {
                try {
                    return new String(Files.readAllBytes(Paths.get(getPolicyDocumentFile())), "UTF-8");
                } catch (Exception err) {
                    throw new BeamException(err.getMessage());
                }
            } else {
                return null;
            }
        }
    }

    public void setPolicyDocumentContents(String policyDocumentContents) {
        this.policyDocumentContents = policyDocumentContents;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPolicyDocumentFile() {
        return this.policyDocumentFile;
    }

    public void setPolicyDocumentFile(String policyDocumentFile) {
        this.policyDocumentFile = policyDocumentFile;
    }

    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        GetPolicyResponse response = client.getPolicy(
                r -> r.policyArn(getPolicyArn())
        );

        if (response.policy() != null) {
            IamPolicyResource newPolicy = new IamPolicyResource();
            newPolicy.setPolicyName(response.policy().policyName());
            setPolicyName(response.policy().policyName());
            System.out.println("Show policy name "  + response.policy().policyName());
            newPolicy.setDescription(response.policy().description());
            setDescription(response.policy().description());
            System.out.println("Show description " + response.policy().description());
            setPolicyArn(response.policy().arn());

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        System.out.println("\nShow the policy name "+this.getPolicyName());
        System.out.println("Show the description name "+this.getDescription());
        System.out.println("Show the policy file name "+this.getPolicyDocumentFile());

        System.out.println("What are the contents "+this.getPolicyDocumentContents());

        CreatePolicyResponse response = client.createPolicy(
                r -> r.policyName(this.getPolicyName())
                        .policyDocument(this.getPolicyDocumentContents())
                        .description(this.getDescription())

        );
        setPolicyArn(response.policy().arn());

        System.out.println("Show the policy arn "+this.getPolicyArn());
        System.out.println("Created a new policy with "+getPolicyName());
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        create();
    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.deletePolicy(r -> r.policyArn(this.getPolicyArn()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getPolicyName() != null) {
            sb.append("policy Name " + getPolicyName());

        } else {
            sb.append("policy Name ");
        }

        return sb.toString();
    }

}