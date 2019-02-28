package gyro.aws.rds;

import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateEventSubscriptionResponse;
import software.amazon.awssdk.services.rds.model.DescribeEventSubscriptionsResponse;
import software.amazon.awssdk.services.rds.model.SubscriptionNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Create a db event subscription.
 *
 * .. code-block:: gyro
 *
 *    aws::db-event-subscription db-event-subscription-example
 *        subscription-name: "db-event-subscription-example"
 *        sns-topic-arn: "arn:aws:sns:us-east-2:242040583208:rds-topic-example"
 *        enabled: true
 *        source-type: "db-instance"
 *        event-categories: ["availability", "deletion"]
 *        tags: {
 *            Name: "db-event-subscription-example"
 *        }
 *    end
 */
@ResourceName("db-event-subscription")
public class DbEventSubscriptionResource extends RdsTaggableResource {

    private Boolean enabled;
    private List<String> eventCategories;
    private String snsTopicArn;
    private List<String> sourceIds;
    private String sourceType;
    private String subscriptionName;

    /**
     * Enable or disable the subscription. Default to true.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * A list of event categories for a SourceType to subscribe to. See `Events <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_Events.html>`_ topic in the Amazon RDS User Guide or by using the `DescribeEventCategories` action.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getEventCategories() {
        if (eventCategories == null || eventCategories.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sorted = new ArrayList<>(eventCategories);
        Collections.sort(sorted);

        return sorted;
    }

    public void setEventCategories(List<String> eventCategories) {
        this.eventCategories = eventCategories;
    }

    /**
     * The ARN of the SNS topic. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getSnsTopicArn() {
        return snsTopicArn;
    }

    public void setSnsTopicArn(String snsTopicArn) {
        this.snsTopicArn = snsTopicArn;
    }

    /**
     * The list of identifiers of the event sources. If omitted, then all sources are included in the response.
     */
    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        if (sourceIds == null) {
            sourceIds = new ArrayList<>();
        }

        this.sourceIds = sourceIds;
    }

    /**
     * The type of source that is generating the events. If omitted, all events are returned. Valid values: ``db-instance``, ``db-cluster``, ``db-parameter-group``, ``db-security-group``, ``db-snapshot``, ``db-cluster-snapshot``.
     */
    @ResourceDiffProperty(updatable = true)
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * The name of the subscription. (Required)
     */
    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getSubscriptionName())) {
            throw new BeamException("subscription-name is missing, unable to load db event subscription.");
        }

        try {
            DescribeEventSubscriptionsResponse response = client.describeEventSubscriptions(
                r -> r.subscriptionName(getSubscriptionName())
            );

            response.eventSubscriptionsList().stream()
                .forEach(s -> {
                    setEnabled(s.enabled());
                    setEventCategories(s.eventCategoriesList());
                    setSnsTopicArn(s.snsTopicArn());
                    List<String> sourceIds = s.sourceIdsList();
                    setSourceIds(sourceIds.isEmpty() ? null : sourceIds);
                    setSourceType(s.sourceType());
                    setArn(s.eventSubscriptionArn());
                }
            );

        } catch (SubscriptionNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        CreateEventSubscriptionResponse response = client.createEventSubscription(
            r -> r.enabled(getEnabled())
                    .eventCategories(getEventCategories())
                    .sourceIds(getSourceIds())
                    .sourceType(getSourceType())
                    .subscriptionName(getSubscriptionName())
                    .snsTopicArn(getSnsTopicArn())
        );

        setArn(response.eventSubscription().eventSubscriptionArn());
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyEventSubscription(
            r -> r.enabled(getEnabled())
                    .eventCategories(getEventCategories())
                    .snsTopicArn(getSnsTopicArn())
                    .sourceType(getSourceType())
                    .subscriptionName(getSubscriptionName())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteEventSubscription(
            r -> r.subscriptionName(getSubscriptionName())
        );

    }

    @Override
    public String toDisplayString() {
        return "db event subscription " + getSubscriptionName();
    }
}
