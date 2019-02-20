package beam.aws.sns;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
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

@ResourceName("subscriber")
public class SubscriberResource extends AwsResource {

    private Map<String, String> subscriptionAttributes;
    private String endpoint;
    private String protocol;
    private String subscriptionArn;
    private String topicArn;

    public SubscriberResource() {}

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getSubscriptionAttributes() {
        if (subscriptionAttributes == null) {
            subscriptionAttributes = new CompactMap<>();
        }

        if (subscriptionAttributes.get("DeliveryPolicy") != null && subscriptionAttributes.get("DeliveryPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(subscriptionAttributes.get("DeliveryPolicy"))), "UTF-8");
                subscriptionAttributes.put("DeliveryPolicy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        if (subscriptionAttributes.get("FilterPolicy") != null && subscriptionAttributes.get("FilterPolicy").endsWith(".json")) {
            try {
                String encode = new String(Files.readAllBytes(Paths.get(subscriptionAttributes.get("FilterPolicy"))), "UTF-8");
                subscriptionAttributes.put("FilterPolicy", formatPolicy(encode));
            } catch (Exception err) {
                throw new BeamException(err.getMessage());
            }
        }

        return subscriptionAttributes;
    }

    public void setSubscriptionAttributes(Map<String, String> subscriptionAttributes) {
        if (this.subscriptionAttributes != null && subscriptionAttributes != null) {
            this.subscriptionAttributes.putAll(subscriptionAttributes);

        } else {
            this.subscriptionAttributes = subscriptionAttributes;
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSubscriptionArn() {
        return subscriptionArn;
    }

    public void setSubscriptionArn(String subscriptionArn) {
        this.subscriptionArn = subscriptionArn;
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
            GetSubscriptionAttributesResponse response = client.getSubscriptionAttributes(r -> r.subscriptionArn(getSubscriptionArn()));

            if (response != null) {
                if (response.attributes().get("DeliveryPolicy") != null) {
                    getSubscriptionAttributes().put("DeliveryPolicy", (response.attributes().get("DeliveryPolicy")));
                }
                if (response.attributes().get("FilterPolicy") != null) {
                    getSubscriptionAttributes().put("FilterPolicy", (response.attributes().get("FilterPolicy")));
                }
                if (response.attributes().get("RawMessageDelivery") != null) {
                    getSubscriptionAttributes().put("RawMessageDelivery", (response.attributes().get("RawMessageDelivery")));
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

        SubscribeResponse subscribeResponse = client.subscribe(r -> r.attributes(getSubscriptionAttributes())
                .endpoint(getEndpoint())
                .protocol(getProtocol())
                .topicArn(getTopicArn()));

        setSubscriptionArn(subscribeResponse.subscriptionArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        SnsClient client = createClient(SnsClient.class);

        client.setSubscriptionAttributes(r -> r.attributeName("DeliveryPolicy")
                                                .attributeValue(getSubscriptionAttributes().get("DeliveryPolicy"))
                                                .subscriptionArn(getSubscriptionArn()));

        client.setSubscriptionAttributes(r -> r.attributeName("FilterPolicy")
                .attributeValue(getSubscriptionAttributes().get("FilterPolicy"))
                .subscriptionArn(getSubscriptionArn()));

        client.setSubscriptionAttributes(r -> r.attributeName("RawMessageDelivery")
                .attributeValue(getSubscriptionAttributes().get("RawMessageDelivery"))
                .subscriptionArn(getSubscriptionArn()));
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
