package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iam.model.GetPolicyResponse;
import software.amazon.awssdk.services.iam.model.GetPolicyVersionResponse;
import software.amazon.awssdk.services.iam.model.PolicyVersion;

import java.net.URLDecoder;
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
 *         policy-name: "rta-test-policy"
 *         description: "testing the policy functionality"
 *         policy-document-file: "policyFile.json"
 *     end
 */

@ResourceName("iam-policy")
public class IamPolicyResource extends AwsResource {
    private String policyName;
    private String description;
    private String pastVersionId;
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPastVersionId() {
        return this.pastVersionId;
    }

    public void setPastVersionId(String pastVersionId) {
        this.pastVersionId = pastVersionId;
    }

    public String getPolicyArn() {
        return this.policyArn;
    }

    public void setPolicyArn(String policyArn) {
        this.policyArn = policyArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPolicyDocumentContents() {
        if (policyDocumentContents != null) {
            return this.policyDocumentContents;
        } else {
            if (getPolicyDocumentFile() != null) {
                try {
                    String encode = new String(Files.readAllBytes(Paths.get(getPolicyDocumentFile())), "UTF-8");
                    return formatPolicy(encode);
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
            setPolicyName(response.policy().policyName());
            setDescription(response.policy().description());
            setPolicyArn(response.policy().arn());

            for (PolicyVersion versions : client.listPolicyVersions(r -> r.policyArn(getPolicyArn())).versions()) {
                setPastVersionId(versions.versionId());
            }

            GetPolicyVersionResponse versionResponse = client.getPolicyVersion(
                r -> r.versionId(getPastVersionId())
                            .policyArn(getPolicyArn())
            );

            String encode = URLDecoder.decode(versionResponse.policyVersion().document());
            setPolicyDocumentContents(formatPolicy(encode));

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        CreatePolicyResponse response = client.createPolicy(
            r -> r.policyName(getPolicyName())
                        .policyDocument(getPolicyDocumentContents())
                        .description(getDescription())
        );

        setPolicyArn(response.policy().arn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        for (PolicyVersion versions : client.listPolicyVersions(r -> r.policyArn(getPolicyArn())).versions()) {
            setPastVersionId(versions.versionId());
        }

        client.createPolicyVersion(
            r -> r.policyArn(getPolicyArn())
                    .policyDocument(getPolicyDocumentContents())
                    .setAsDefault(true)
        );

        client.deletePolicyVersion(
            r -> r.policyArn(getPolicyArn())
                        .versionId(getPastVersionId())
        );
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
            sb.append("policy name " + getPolicyName());

        } else {
            sb.append("policy name ");
        }

        return sb.toString();
    }

    public String formatPolicy(String policy) {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }

}