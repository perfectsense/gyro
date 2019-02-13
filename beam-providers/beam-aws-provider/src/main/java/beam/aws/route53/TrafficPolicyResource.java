package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateTrafficPolicyResponse;
import software.amazon.awssdk.services.route53.model.GetTrafficPolicyResponse;
import software.amazon.awssdk.services.route53.model.TrafficPolicy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Creates a Traffic Policy resource.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::traffic-policy traffic-policy-example
 *         name: "traffic-policy-example"
 *         comment: "traffic-policy-example Comment"
 *         document-path: "policy.json"
 *     end
 *
 */
@ResourceName("traffic-policy")
public class TrafficPolicyResource extends AwsResource {
    private String name;
    private String comment;
    private String document;
    private String documentPath;
    private String trafficPolicyId;

    /**
     * The name of the traffic policy. (Required)
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The comment you want to put with the policy.
     */
    @ResourceDiffProperty(updatable = true)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * The policy document. Required unless document path provided.
     */
    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    /**
     * The policy document file path. Required unless document provided.
     */
    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;

        if (documentPath != null) {
            setDocumentFromPath();
        }
    }

    public String getTrafficPolicyId() {
        return trafficPolicyId;
    }

    public void setTrafficPolicyId(String trafficPolicyId) {
        this.trafficPolicyId = trafficPolicyId;
    }

    @Override
    public boolean refresh() {
        Route53Client client = createClient(Route53Client.class);

        GetTrafficPolicyResponse response = client.getTrafficPolicy(
            r -> r.id(getTrafficPolicyId())
        );

        TrafficPolicy trafficPolicy = response.trafficPolicy();
        setName(trafficPolicy.name());
        setComment(trafficPolicy.comment());
        setDocument(trafficPolicy.document());

        return true;
    }

    @Override
    public void create() {
        Route53Client client = createClient(Route53Client.class);

        CreateTrafficPolicyResponse response = client.createTrafficPolicy(
            r -> r.name(getName())
                .comment(getComment())
                .document(getDocument())
        );

        TrafficPolicy trafficPolicy = response.trafficPolicy();
        setTrafficPolicyId(trafficPolicy.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Route53Client client = createClient(Route53Client.class);

        client.updateTrafficPolicyComment(
            r -> r.id(getTrafficPolicyId())
                .comment(getComment())
        );
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class);

        client.deleteTrafficPolicy(
            r -> r.id(getTrafficPolicyId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("traffic policy");

        if (!ObjectUtils.isBlank(getTrafficPolicyId())) {
            sb.append(" - ").append(getTrafficPolicyId());

        }

        return sb.toString();
    }

    private void setDocumentFromPath() {
        try {
            String dir = scope().getFileScope().getFile().substring(0, scope().getFileScope().getFile().lastIndexOf(File.separator));
            setDocument(new String(Files.readAllBytes(Paths.get(dir + File.separator + getDocumentPath())), StandardCharsets.UTF_8));
        } catch (IOException ioex) {
            throw new BeamException(String.format("traffic policy - %s document error."
                + " Unable to read document from path [%s]", getName(), getDocument()));
        }
    }
}
