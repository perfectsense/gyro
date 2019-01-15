package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ResourceName("auto-scaling-group")
public class AutoScalingGroupResource extends AwsResource {

    private String autoScalingGroupName;
    private String launchTemplateId;
    private List<String> availabilityZones;
    private Integer maxSize;
    private Integer minSize;
    private Integer desiredCapacity;
    private Integer defaultCooldown;
    private String healthCheckType;
    private Integer healthCheckGracePeriod;
    private String launchConfigurationName;
    private Boolean newInstancesProtectedFromScaleIn;
    private List<String> subnetIds;

    private String arn;

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

    public List<String> getAvailabilityZones() {
        if (availabilityZones == null) {
            availabilityZones = new ArrayList<>();
        }
        return availabilityZones;
    }

    public void setAvailabilityZones(List<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
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

    public Integer getDesiredCapacity() {
        if (desiredCapacity == null) {
            desiredCapacity = 0;
        }
        return desiredCapacity;
    }

    public void setDesiredCapacity(Integer desiredCapacity) {
        this.desiredCapacity = desiredCapacity;
    }

    public Integer getDefaultCooldown() {
        if (defaultCooldown == null) {
            defaultCooldown = 0;
        }
        return defaultCooldown;
    }

    public void setDefaultCooldown(Integer defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }

    public String getHealthCheckType() {
        if (healthCheckType == null) {
            healthCheckType = "EC2";
        }
        return healthCheckType.toUpperCase();
    }

    public void setHealthCheckType(String healthCheckType) {
        this.healthCheckType = healthCheckType;
    }

    public Integer getHealthCheckGracePeriod() {
        if (healthCheckGracePeriod == null) {
            healthCheckGracePeriod = 0;
        }
        return healthCheckGracePeriod;
    }

    public void setHealthCheckGracePeriod(Integer healthCheckGracePeriod) {
        this.healthCheckGracePeriod = healthCheckGracePeriod;
    }

    public String getLaunchConfigurationName() {
        return launchConfigurationName;
    }

    public void setLaunchConfigurationName(String launchConfigurationName) {
        this.launchConfigurationName = launchConfigurationName;
    }

    public Boolean getNewInstancesProtectedFromScaleIn() {
        if (newInstancesProtectedFromScaleIn == null) {
            newInstancesProtectedFromScaleIn = false;
        }
        return newInstancesProtectedFromScaleIn;
    }

    public void setNewInstancesProtectedFromScaleIn(Boolean newInstancesProtectedFromScaleIn) {
        this.newInstancesProtectedFromScaleIn = newInstancesProtectedFromScaleIn;
    }

    public List<String> getSubnetIds() {
        if (subnetIds == null) {
            subnetIds = new ArrayList<>();
        }
        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    @Override
    public boolean refresh() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        AutoScalingGroup autoScalingGroup = getAutoScalingGroup(client);

        if (autoScalingGroup == null) {
            return false;
        }

        setArn(autoScalingGroup.autoScalingGroupARN());
        setLaunchTemplateId(autoScalingGroup.launchTemplate() == null ? null : autoScalingGroup.launchTemplate().launchTemplateId());
        setMaxSize(autoScalingGroup.maxSize());
        setMinSize(autoScalingGroup.minSize());
        setAvailabilityZones(autoScalingGroup.availabilityZones());
        setDesiredCapacity(autoScalingGroup.desiredCapacity());
        setDefaultCooldown(autoScalingGroup.defaultCooldown());
        setHealthCheckType(autoScalingGroup.healthCheckType());
        setHealthCheckGracePeriod(autoScalingGroup.healthCheckGracePeriod());
        setLaunchConfigurationName(autoScalingGroup.launchConfigurationName());
        setNewInstancesProtectedFromScaleIn(autoScalingGroup.newInstancesProtectedFromScaleIn());

        return true;
    }

    @Override
    public void create() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();

        client.createAutoScalingGroup(
            o -> o.autoScalingGroupName(getAutoScalingGroupName())
                .launchTemplate(LaunchTemplateSpecification.builder().launchTemplateId(getLaunchTemplateId()).build())
                .maxSize(getMaxSize())
                .minSize(getMinSize())
                .availabilityZones(getAvailabilityZones())
                .desiredCapacity(getDesiredCapacity())
                .defaultCooldown(getDefaultCooldown())
                .healthCheckType(getHealthCheckType())
                .healthCheckGracePeriod(getHealthCheckGracePeriod())
                .launchConfigurationName(getLaunchConfigurationName())
                .newInstancesProtectedFromScaleIn(getNewInstancesProtectedFromScaleIn())
                .vpcZoneIdentifier(getSubnetIds().isEmpty() ? "" : StringUtils.join(getSubnetIds(), ","))
        );

        AutoScalingGroup autoScalingGroup = getAutoScalingGroup(client);

        if (autoScalingGroup != null) {
            setArn(autoScalingGroup.autoScalingGroupARN());
        }
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

    private AutoScalingGroup getAutoScalingGroup(AutoScalingClient client) {
        if (ObjectUtils.isBlank(getAutoScalingGroupName())) {
            throw new BeamException("auto-scale-group-name is missing, unable to load auto scale group.");
        }

        try {
            DescribeAutoScalingGroupsResponse response = client.describeAutoScalingGroups(
                r -> r.autoScalingGroupNames(Collections.singleton(getAutoScalingGroupName()))
            );

            if (response.autoScalingGroups().isEmpty()) {
                return null;
            }

            return response.autoScalingGroups().get(0);
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return null;
            }

            throw ex;
        }
    }

    private void validate() {
        if (!getHealthCheckType().equals("ELB") && !getHealthCheckType().equals("EC2")) {
            throw new BeamException("The value - (" + getHealthCheckType() + ") is invalid for parameter Health Check Type.");
        }
    }
}
