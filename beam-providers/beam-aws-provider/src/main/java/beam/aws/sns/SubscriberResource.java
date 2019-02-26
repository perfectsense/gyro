package beam.aws.sns;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.core.diff.ResourceOutput;
import beam.lang.Resource;
import com.psddev.dari.util.CompactMap;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesResponse;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Creates a subscriber to a topic
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::subscriber subscriber-example
 *         protocol: "sqs"
 *         endpoint: $(aws::sqs sqs-example | queue-arn)
 *         attributes: {
 *             FilterPolicy: "beam-providers/beam-aws-provider/examples/sns/filter-policy.json",
 *             RawMessageDelivery: "true"
 *         }
 *         topic-arn: $(aws::topic sns-topic-example | topic-arn)
 *     end
 */
@ResourceName("subscriber")
public class SubscriberResource extends AwsResource {

    private Map<String, String> attributes;
    private String endpoint;
    private String protocol;
    private String subscriptionArn;
    private String topicArn;

    public SubscriberResource() {}

    /**
     * The attributes for the subscription (Optional)
     *
     * Possible attributes are DeliveryPolicy, FilterPolicy, and RawMessageDelivery
     *
     * DeliveryPolicy can be a json file path or json blob (Optional)
     *
     * FilterPolicy can be a json file path or json blob (Optional)
     *
     * RawMessageDelivery is a boolean (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new CompactMap<>();
        }

        if (attributes.get("DeliveryPolicy") != null && attributes.get("DeliveryPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(attributes.get("DeliveryPolicy"))), "UTF-8");
                attributes.put("DeliveryPolicy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        if (attributes.get("FilterPolicy") != null && attributes.get("FilterPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(attributes.get("FilterPolicy"))), "UTF-8");
                attributes.put("FilterPolicy", formatPolicy(encode));
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
     * The endpoint of the resource subscribed to the topic. (Required)
     */
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * The protocol associated with the endpoint. (Required)
     */
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ResourceOutput
    public String getSubscriptionArn() {
        return subscriptionArn;
    }

    public void setSubscriptionArn(String subscriptionArn) {
        this.subscriptionArn = subscriptionArn;
    }

    /**
     * The topic arn to subscribe to. (Required)
     */
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
            GetSubscriptionAttributesResponse response = client.getSubscriptionAttributes(r -> r.subscriptionArn(getSubscriptionArn()));
            getAttributes().clear();

            //The list of attributes is much larger than what can be set.
            //Only those that can be set are extracted out of the list of attributes.
            if (response != null) {
                if (response.attributes().get("DeliveryPolicy") != null) {
                    getAttributes().put("DeliveryPolicy", (response.attributes().get("DeliveryPolicy")));
                }
                if (response.attributes().get("FilterPolicy") != null) {
                    getAttributes().put("FilterPolicy", (response.attributes().get("FilterPolicy")));
                }
                if (response.attributes().get("RawMessageDelivery") != null) {
                    getAttributes().put("RawMessageDelivery", (response.attributes().get("RawMessageDelivery")));
                }

                setTopicArn(response.attributes().get("TopicArn"));
            }
        } catch (NotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void create() {
        SnsClient client = createClient(SnsClient.class);

        SubscribeResponse subscribeResponse = client.subscribe(r -> r.attributes(getAttributes())
                .endpoint(getEndpoint())
                .protocol(getProtocol())
                .topicArn(getTopicArn()));

        setSubscriptionArn(subscribeResponse.subscriptionArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        SnsClient client = createClient(SnsClient.class);

        for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
            client.setSubscriptionAttributes(r -> r.attributeName(entry.getKey())
                    .attributeValue(getAttributes().get(entry.getValue()))
                    .subscriptionArn(getSubscriptionArn()));
        }
    }

    @Override
    public void delete() {
        SnsClient client = createClient(SnsClient.class);

        client.unsubscribe(r -> r.subscriptionArn(getSubscriptionArn()));
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("subscriber with protocol " + getProtocol());
        if (getEndpoint() != null) {
            sb.append(" and endpoint " + getEndpoint());
        }
        return sb.toString();
    }

    private String formatPolicy(String policy) {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }
}
