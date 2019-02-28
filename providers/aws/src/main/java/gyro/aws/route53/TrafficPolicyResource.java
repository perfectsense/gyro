package gyro.aws.route53;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateTrafficPolicyResponse;
import software.amazon.awssdk.services.route53.model.CreateTrafficPolicyVersionResponse;
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
 * .. code-block:: gyro
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
    private Integer version;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean refresh() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        GetTrafficPolicyResponse response = client.getTrafficPolicy(
            r -> r.id(getTrafficPolicyId()).version(getVersion())
        );

        TrafficPolicy trafficPolicy = response.trafficPolicy();
        setName(trafficPolicy.name());
        setComment(trafficPolicy.comment());
        setDocument(trafficPolicy.document());

        return true;
    }

    @Override
    public void create() {
        validate(true);

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        TrafficPolicy trafficPolicy = null;

        if (ObjectUtils.isBlank(getTrafficPolicyId())) {
            CreateTrafficPolicyResponse response = client.createTrafficPolicy(
                r -> r.name(getName())
                    .comment(getComment())
                    .document(getDocument())
            );

            trafficPolicy = response.trafficPolicy();

        } else {
            CreateTrafficPolicyVersionResponse response = client.createTrafficPolicyVersion(
                r -> r.comment(getComment())
                    .id(getTrafficPolicyId())
                    .document(getDocument())
            );

            trafficPolicy = response.trafficPolicy();
        }

        setTrafficPolicyId(trafficPolicy.id());
        setVersion(trafficPolicy.version());
        setName(trafficPolicy.name());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        validate(false);

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        client.updateTrafficPolicyComment(
            r -> r.id(getTrafficPolicyId())
                .comment(getComment())
                .version(getVersion())
        );
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        client.deleteTrafficPolicy(
            r -> r.id(getTrafficPolicyId())
                .version(getVersion())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("traffic policy");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(" - ").append(getName());
        }

        if (getVersion() != null) {
            sb.append(" - version: ").append(getVersion());
        }

        if (ObjectUtils.isBlank(getName()) && !ObjectUtils.isBlank(getTrafficPolicyId())) {
            sb.append(" [ from - ").append(getTrafficPolicyId()).append(" ]");
        } else if (!ObjectUtils.isBlank(getTrafficPolicyId())) {
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

    private void validate(boolean isCreate) {
        if ((ObjectUtils.isBlank(getName()) && ObjectUtils.isBlank(getTrafficPolicyId()))
            || (isCreate && !ObjectUtils.isBlank(getName()) && !ObjectUtils.isBlank(getTrafficPolicyId()))) {
            throw new BeamException("Either param 'name' or 'traffic-policy-id' need to be provided, but not both.");
        }
    }
}
