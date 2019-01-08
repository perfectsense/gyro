package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iam.model.CreatePolicyVersionResponse;
import software.amazon.awssdk.services.iam.model.GetPolicyResponse;
import software.amazon.awssdk.services.iam.model.Policy;
import software.amazon.awssdk.services.iam.model.SetDefaultPolicyVersionResponse;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Creates a Policy with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::policyResource example-role
 *         roleName: $(aws::roleResource example-role | roleName)
 *         description: testing the policy functionality
 *         policyName: rta-test-policy
 *         policyDocumentFile: policyFile.json
 *
 *     end
 */

@ResourceName("policy-resource")
public class IamPolicyResource extends AwsResource {
    private String policyName;
    private String description;
    private String policyArn;
    private Map<String, Object> policyDocument;
    private File policyDocumentFile;

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
    public Map<String, Object> getPolicyDocument() {
        if (this.policyDocument != null) {
            return this.policyDocument;
        } else {
            File var1 = this.getPolicyDocumentFile();
            if (var1 != null) {
                try {
                    return (Map)ObjectUtils.fromJson(IoUtils.toString(var1, Charsets.UTF_8));
                } catch (IOException var3) {
                    throw Throwables.propagate(var3);
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
        return this.policyDocumentFile;
    }

    public void setPolicyDocumentFile(File policyDocumentFile) {
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

        GetPolicyVersionResponse versionResponse = client.getPolicyVersion(
                r -> r.policyArn(getPolicyArn())
                        .versionId("v1")
        );

        if (response.policy() != null) {
            IamPolicyResource newPolicy = new IamPolicyResource();
            newPolicy.setPolicyName(response.policy().policyName());
            setPolicyName(response.policy().policyName());
            newPolicy.setDescription(response.policy().description());
            setDescription(response.policy().description());

            for(PolicyVersion versions : client.listPolicyVersions(r -> r.policyArn(getPolicyArn())).versions()){
                newPolicy.setPolicyDocument((Map)ObjectUtils.fromJson(StringUtils.decodeUri(versions.document())));
                setPolicyDocument((Map)ObjectUtils.fromJson(StringUtils.decodeUri(versions.document())));
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

        System.out.println("\nShow the policy name "+this.getPolicyName());
        System.out.println("Show the description name "+this.getDescription());
        System.out.println("Show the policy file name "+this.getPolicyDocumentFile());
        System.out.println("Show the policy document "+this.getPolicyDocument());


        CreatePolicyResponse response = client.createPolicy(
                r -> r.policyName(this.getPolicyName())
                        .policyDocument(ObjectUtils.toJson(this.getPolicyDocument()))
                        .description(this.getDescription())

        );
        setPolicyArn(response.policy().arn());

        CreatePolicyVersionResponse versionResponse = client.createPolicyVersion(
                r -> r.policyArn(getPolicyArn())
                        .policyDocument(ObjectUtils.toJson(this.getPolicyDocument()))
                        .setAsDefault(true)

        );

        SetDefaultPolicyVersionResponse defaultResponse = client.setDefaultPolicyVersion(
                r -> r.policyArn(getPolicyArn())
                        .versionId("v1"));

        System.out.println("Show the policy arn "+this.getPolicyArn());
        System.out.println("Created a new policy with "+getPolicyName());
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        delete();
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
            sb.append("Policy Name " + getPolicyName());

        } else {
            sb.append("Policy Name ");
        }

        return sb.toString();
    }

}