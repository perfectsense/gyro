package beam.aws.autoscaling;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.NotificationConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ResourceName(parent = "auto-scaling-group", value = "auto-scaling-notification")
public class AutoScalingGroupNotificationResource extends AwsResource {

    private String topicArn;
    private String autoScalingGroupName;
    private List<String> notificationTypes;

    /**
     * The ARN of the SNS topic to notify. (Required)
     */
    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    /**
     * The name of the parent auto scaling group. (Auto populated)
     */
    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public void setAutoScalingGroupName(String autoScalingGroupName) {
        this.autoScalingGroupName = autoScalingGroupName;
    }

    /**
     * The event on which to notify. Valid values [ 'autoscaling:EC2_INSTANCE_LAUNCH', 'autoscaling:EC2_INSTANCE_LAUNCH_ERROR'
     * 'autoscaling:EC2_INSTANCE_TERMINATE', 'autoscaling:EC2_INSTANCE_TERMINATE_ERROR' ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getNotificationTypes() {
        if (notificationTypes == null) {
            notificationTypes = new ArrayList<>();
        }

        return notificationTypes;
    }

    public void setNotificationTypes(List<String> notificationTypes) {
        this.notificationTypes = notificationTypes;
    }

    public AutoScalingGroupNotificationResource() {

    }

    public AutoScalingGroupNotificationResource(NotificationConfiguration notificationConfiguration) {
        setAutoScalingGroupName(notificationConfiguration.autoScalingGroupName());
        setTopicArn(notificationConfiguration.topicARN());
        setNotificationTypes(Collections.singletonList(notificationConfiguration.notificationType()));
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();
        setAutoScalingGroupName(getParentId());
        saveNotification(client);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();
        saveNotification(client);
    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.deleteNotificationConfiguration(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
            .topicARN(getTopicArn())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("auto scale notification");

        if (!ObjectUtils.isBlank(getTopicArn())) {
            sb.append(" - ").append(getTopicArn());
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s", getTopicArn());
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }

    private String getParentId() {
        AutoScalingGroupResource parent = (AutoScalingGroupResource) parentResource();
        if (parent == null) {
            throw new BeamException("Parent Auto Scale Group resource not found.");
        }
        return parent.getAutoScalingGroupName();
    }

    private void saveNotification(AutoScalingClient client) {
        client.putNotificationConfiguration(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
                .notificationTypes(getNotificationTypes())
                .topicARN(getTopicArn())
        );
    }

    private void validate() {
        HashSet<String> validNotificationSet = new HashSet<>(
            Arrays.asList(
                "autoscaling:EC2_INSTANCE_LAUNCH",
                "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
                "autoscaling:EC2_INSTANCE_TERMINATE",
                "autoscaling:EC2_INSTANCE_TERMINATE_ERROR"
            )
        );

        if (getNotificationTypes().isEmpty() || getNotificationTypes().size() > 1 || !validNotificationSet.contains(getNotificationTypes().get(0))) {
            throw new BeamException("The param 'notification-types' needs one value."
                + " Valid values [ '" + String.join("', '", validNotificationSet) + "' ].");
        }
    }
}
