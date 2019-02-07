package beam.aws.autoscaling;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.DescribeScheduledActionsResponse;
import software.amazon.awssdk.services.autoscaling.model.ScheduledUpdateGroupAction;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

@ResourceName(parent = "auto-scaling-group", value = "scheduled-action")
public class AutoScalingGroupScheduledActionResource extends AwsResource {

    private String scheduledActionName;
    private String autoScalingGroupName;
    private Integer desiredCapacity;
    private Integer maxSize;
    private Integer minSize;
    private String recurrence;
    private Date startTime;
    private Date endTime;
    private String arn;

    /**
     * The scheduled action name. (Required)
     */
    public String getScheduledActionName() {
        return scheduledActionName;
    }

    public void setScheduledActionName(String scheduledActionName) {
        this.scheduledActionName = scheduledActionName;
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
     * The desired capacity of instances this scheduled action to scale to.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getDesiredCapacity() {
        return desiredCapacity;
    }

    public void setDesiredCapacity(Integer desiredCapacity) {
        this.desiredCapacity = desiredCapacity;
    }

    /**
     * The maximum number of instances this scheduled action to scale to.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * The minimum number of instances this scheduled action to scale to.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    /**
     * The recurring schedule for this action, in Unix cron syntax format
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    /**
     * The time for this action to start
     */
    @ResourceDiffProperty(updatable = true)
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * The time for this action to start
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public AutoScalingGroupScheduledActionResource() {

    }

    public AutoScalingGroupScheduledActionResource(ScheduledUpdateGroupAction scheduledUpdateGroupAction) {
        setScheduledActionName(scheduledUpdateGroupAction.scheduledActionName());
        setAutoScalingGroupName(scheduledUpdateGroupAction.autoScalingGroupName());
        setDesiredCapacity(scheduledUpdateGroupAction.desiredCapacity());
        setMaxSize(scheduledUpdateGroupAction.maxSize());
        setMinSize(scheduledUpdateGroupAction.minSize());
        setRecurrence(scheduledUpdateGroupAction.recurrence());
        setStartTime(scheduledUpdateGroupAction.startTime() != null ? Date.from(scheduledUpdateGroupAction.startTime()) : null);
        setEndTime(scheduledUpdateGroupAction.endTime() != null ? Date.from(scheduledUpdateGroupAction.endTime()) : null);
        setArn(scheduledUpdateGroupAction.scheduledActionARN());
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
        saveScheduledAction(client);

        //set arn
        DescribeScheduledActionsResponse response = client.describeScheduledActions(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
            .scheduledActionNames(Collections.singleton(getScheduledActionName()))
        );

        if (!response.scheduledUpdateGroupActions().isEmpty()) {
            setArn(response.scheduledUpdateGroupActions().get(0).scheduledActionARN());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();
        saveScheduledAction(client);
    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.deleteScheduledAction(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
            .scheduledActionName(getScheduledActionName())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("scheduled action");

        if (!ObjectUtils.isBlank(getScheduledActionName())) {
            sb.append(" - ").append(getScheduledActionName());
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s", getScheduledActionName());
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

    private void saveScheduledAction(AutoScalingClient client) {
        client.putScheduledUpdateGroupAction(
            r -> r.scheduledActionName(getScheduledActionName())
                .autoScalingGroupName(getAutoScalingGroupName())
                .desiredCapacity(getDesiredCapacity())
                .maxSize(getMaxSize())
                .minSize(getMinSize())
                .recurrence(getRecurrence())
                .startTime(getStartTime() != null ? getStartTime().toInstant() : null)
                .endTime(getEndTime() != null ? getEndTime().toInstant() : null)
        );
    }

    private void validate() {
        if (getMaxSize() == null && getMinSize() == null && getDesiredCapacity() == null) {
            throw new BeamException("At least one of the params 'max-size' or 'min-size' or 'desired-capacity' needs to be provided.");
        }

        if (getMinSize() != null && getMinSize() < 0) {
            throw new BeamException("The value - (" + getMinSize()
                + ") is invalid for parameter 'min-size'. Valid values [ Integer value grater or equal to 0 ].");
        }

        if (getMaxSize() != null && getMaxSize() < 0) {
            throw new BeamException("The value - (" + getMaxSize()
                + ") is invalid for parameter 'max-size'. Valid values [ Integer value grater or equal to 0 ].");
        }

        if (getDesiredCapacity() != null && getDesiredCapacity() < 0) {
            throw new BeamException("The value - (" + getDesiredCapacity()
                + ") is invalid for parameter 'desired-capacity'. Valid values [ Integer value grater or equal to 0 ].");
        }

        if (getMinSize() != null && getMaxSize() != null && (getMaxSize() < getMinSize())) {
            throw new BeamException("The value - (" + getMinSize()
                + ") for parameter 'min-size' needs to be equal or smaller than the value - (" + getMaxSize()
                + ") for parameter 'max-size'.");
        }
    }
}
