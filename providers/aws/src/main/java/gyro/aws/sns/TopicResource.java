package gyro.aws.sns;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;
import com.psddev.dari.util.CompactMap;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.NotFoundException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Creates a sns topic
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     aws::topic sns-topic-example
 *         attributes: {
 *             DisplayName: "sns-topic-example",
 *             Policy: "gyro-providers/gyro-aws-provider/examples/sns/sns-policy.json"
 *         }
 *         name: "sns-topic"
 *     end
 */
@ResourceName("topic")
public class TopicResource extends AwsResource {

    private Map<String, String> attributes;
    private String name;
    private String topicArn;

    /**
     * The attributes associated with this topic (Required)
     *
     * Possible attributes are DeliveryPolicy, Policy, and DisplayName
     *
     * DeliveryPolicy can be a json file path or json blob (Optional)
     *
     * DisplayName is a string (Required)
     *
     * Policy can be a json file path or json blob (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new CompactMap<>();
        }

        if (attributes.get("Policy") != null && attributes.get("Policy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(attributes.get("Policy"))), "UTF-8");
                attributes.put("Policy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        if (attributes.get("DeliveryPolicy") != null && attributes.get("DeliveryPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(attributes.get("DeliveryPolicy"))), "UTF-8");
                attributes.put("DeliveryPolicy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        if (this.attributes != null && attributes != null) {
            this.attributes.putAll(attributes);

        } else {
            this.attributes = attributes;
        }
    }

    /**
     * The name of the topic. (Required)
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceOutput
    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    @Override
    public boolean refresh() {
        SnsClient client = createClient(SnsClient.class);

        try {
            GetTopicAttributesResponse attributesResponse = client.getTopicAttributes(r -> r.topicArn(getTopicArn()));
            getAttributes().clear();

            //The list of attributes is much larger than what can be set.
            //Only those that can be set are extracted out of the list of attributes.
            if (attributesResponse.attributes().get("DisplayName") != null) {
                getAttributes().put("DisplayName", (attributesResponse.attributes().get("DisplayName")));
            }
            if (attributesResponse.attributes().get("Policy") != null) {
                getAttributes().put("Policy", (attributesResponse.attributes().get("Policy")));
            }
            if (attributesResponse.attributes().get("DeliveryPolicy") != null) {
                getAttributes().put("DeliveryPolicy", (attributesResponse.attributes().get("DeliveryPolicy")));
            }

            setTopicArn(attributesResponse.attributes().get("TopicArn"));

            return true;

        } catch (AuthorizationErrorException | InvalidParameterException ex) {
            throw new BeamException(ex.getMessage());
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public void create() {
        SnsClient client = createClient(SnsClient.class);

        CreateTopicResponse topicResponse = client.createTopic(r -> r.attributes(getAttributes())
                                    .name(getName()));

        setTopicArn(topicResponse.topicArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        SnsClient client = createClient(SnsClient.class);

        for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
            client.setTopicAttributes(r -> r.attributeName(entry.getKey())
                    .attributeValue(entry.getValue())
                    .topicArn(getTopicArn()));
        }
    }

    @Override
    public void delete() {
        SnsClient client = createClient(SnsClient.class);

        client.deleteTopic(r -> r.topicArn(getTopicArn()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sns topic ");

        if (getTopicArn() != null) {
            sb.append(getTopicArn());
        }

        return sb.toString();
    }

    private String formatPolicy(String policy) {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }
}
