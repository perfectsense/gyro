package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;

import java.util.Collections;
import java.util.Set;

@ResourceName("auto-scaling-group")
public class AutoScalingGroupResource extends AwsResource {

    private String autoScalingGroupName;
    private String launchTemplateId;
    private String availabilityZone;
    private Integer maxSize;
    private Integer minSize;

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public void setAutoScalingGroupName(String autoScalingGroupName) {
        this.autoScalingGroupName = autoScalingGroupName;
    }

    public String getLaunchTemplateId() {
        return launchTemplateId;
    }

    public void setLaunchTemplateId(String launchTemplateId) {
        this.launchTemplateId = launchTemplateId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Integer getMaxSize() {
        if (maxSize == null) {
            maxSize = 0;
        }
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getMinSize() {
        if (minSize == null) {
            minSize = 0;
        }
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.createAutoScalingGroup(
            o -> o.autoScalingGroupName(getAutoScalingGroupName())
                .launchTemplate(LaunchTemplateSpecification.builder().launchTemplateId(getLaunchTemplateId()).build())
                .maxSize(getMaxSize())
                .minSize(getMinSize())
                .availabilityZones(Collections.singleton(getAvailabilityZone()))
        );

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.deleteAutoScalingGroup(r -> r.autoScalingGroupName(getAutoScalingGroupName()).forceDelete(true));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Auto Scale Group");

        if (!ObjectUtils.isBlank(getAutoScalingGroupName())) {
            sb.append(" - ").append(getAutoScalingGroupName());

        }

        return sb.toString();
    }
}
