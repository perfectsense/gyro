package beam.aws.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DeleteScheduledActionRequest;
import com.amazonaws.services.autoscaling.model.PutScheduledUpdateGroupActionRequest;
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import net.redhogs.cronparser.CronExpressionDescriptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class AutoScalingGroupScheduleResource extends AWSResource<ScheduledUpdateGroupAction> {

    private BeamReference autoScalingGroup;
    private String name;
    private Integer min;
    private Integer max;
    private Integer desired;
    private String recurrence;
    private Date startTime;
    private Date endTime;

    public BeamReference getAutoScalingGroup() {
        return newParentReference(AutoScalingGroupResource.class, autoScalingGroup);
    }

    public void setAutoScalingGroup(BeamReference autoScalingGroup) {
        this.autoScalingGroup = autoScalingGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getDesired() {
        return desired;
    }

    public void setDesired(Integer desired) {
        this.desired = desired;
    }

    @ResourceDiffProperty(updatable = true)
    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    @ResourceDiffProperty(updatable = true)
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @ResourceDiffProperty(updatable = true)
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, ScheduledUpdateGroupAction schedule) {
        setName(schedule.getScheduledActionName());
        setDesired(schedule.getDesiredCapacity());
        setMax(schedule.getMaxSize());
        setMin(schedule.getMinSize());
        setRecurrence(schedule.getRecurrence());
        setStartTime(schedule.getStartTime());
        setEndTime(schedule.getEndTime());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());

        PutScheduledUpdateGroupActionRequest scheduleRequest = new PutScheduledUpdateGroupActionRequest();
        scheduleRequest.setAutoScalingGroupName(getAutoScalingGroup().awsId());
        scheduleRequest.setScheduledActionName(getName());
        scheduleRequest.setRecurrence(getRecurrence());
        scheduleRequest.setStartTime(getStartTime());
        scheduleRequest.setEndTime(getEndTime());
        scheduleRequest.setMaxSize(getMax());
        scheduleRequest.setMinSize(getMin());
        scheduleRequest.setDesiredCapacity(getDesired());

        client.putScheduledUpdateGroupAction(scheduleRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, ScheduledUpdateGroupAction> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());

        DeleteScheduledActionRequest deleteRequest = new DeleteScheduledActionRequest();
        deleteRequest.setAutoScalingGroupName(getAutoScalingGroup().awsId());
        deleteRequest.setScheduledActionName(getName());

        client.deleteScheduledAction(deleteRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("scheduled action (");
        sb.append(getName());
        sb.append(") ");

        List<String> attributes = new ArrayList<>();

        if (getStartTime() != null) {
            attributes.add("executes at " + getStartTime());
        }

        if (getEndTime() != null) {
            attributes.add("ends on " + getEndTime());
        }

        if (getRecurrence() != null) {
            String description = getRecurrence();
            try {
                description = CronExpressionDescriptor.getDescription(getRecurrence());
            } catch (Exception ex) {

            }

            attributes.add("recurrence: [" + description + " (UTC Time)]");
        }

        if (getMin() != null) {
            attributes.add("min: " + getMin());
        }

        if (getMax() != null) {
            attributes.add("max: " + getMax());
        }

        if (getDesired() != null) {
            attributes.add("desired: " + getDesired());
        }

        sb.append(Joiner.on(", ").join(attributes));

        return sb.toString();
    }

}