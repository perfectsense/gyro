package beam.aws.sns;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
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

@ResourceName("sns")
public class SnsResource extends AwsResource {

    private Map<String, String> topicAttributes;
    private String name;
    private String topicArn;

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTopicAttributes() {
        if (topicAttributes == null) {
            topicAttributes = new CompactMap<>();
        }

        if (topicAttributes.get("Policy") != null && topicAttributes.get("Policy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(topicAttributes.get("Policy"))), "UTF-8");
                topicAttributes.put("Policy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        if (topicAttributes.get("DeliveryPolicy") != null && topicAttributes.get("DeliveryPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(topicAttributes.get("DeliveryPolicy"))), "UTF-8");
                topicAttributes.put("DeliveryPolicy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        return topicAttributes;
    }

    public void setTopicAttributes(Map<String, String> topicAttributes) {
        if (this.topicAttributes != null && topicAttributes != null) {
            this.topicAttributes.putAll(topicAttributes);

        } else {
            this.topicAttributes = topicAttributes;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
            getTopicAttributes().clear();

            if (attributesResponse.attributes().get("DisplayName") != null) {
                getTopicAttributes().put("DisplayName", (attributesResponse.attributes().get("DisplayName")));
            }
            if (attributesResponse.attributes().get("Policy") != null) {
                getTopicAttributes().put("Policy", (attributesResponse.attributes().get("Policy")));
            }
            if (attributesResponse.attributes().get("DeliveryPolicy") != null) {
                getTopicAttributes().put("DeliveryPolicy", (attributesResponse.attributes().get("DeliveryPolicy")));
            }

            setTopicArn(attributesResponse.attributes().get("TopicArn"));

            return true;

        } catch (NotFoundException ex) {
            return false;
        } catch (AuthorizationErrorException exception) {
            return false;
        } catch (InvalidParameterException ex) {
            return false;
        }
    }

    @Override
    public void create() {
        SnsClient client = createClient(SnsClient.class);

        CreateTopicResponse topicResponse = client.createTopic(r -> r.attributes(getTopicAttributes())
                                    .name(getName()));

        setTopicArn(topicResponse.topicArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        SnsClient client = createClient(SnsClient.class);

        client.setTopicAttributes(r -> r.attributeName("DeliveryPolicy")
                .attributeValue(getTopicAttributes().get("DeliveryPolicy"))
                .topicArn(getTopicArn()));

        client.setTopicAttributes(r -> r.attributeName("DisplayName")
                .attributeValue(getTopicAttributes().get("DisplayName"))
                .topicArn(getTopicArn()));

        client.setTopicAttributes(r -> r.attributeName("Policy")
                .attributeValue(getTopicAttributes().get("Policy"))
                .topicArn(getTopicArn()));
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
