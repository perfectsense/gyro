package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Creates an Auto scaling Group from a Launch Configuration or from a Launch Template.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::auto-scaling-group auto-scaling-group-example
 *         auto-scaling-group-name: "auto-scaling-group-beam-1"
 *         launch-configuration-name: $(aws::launch-configuration launch-configuration-auto-scaling-group-example | launch-configuration-name)
 *         availability-zones: [
 *             $(aws::subnet subnet-auto-scaling-group-example | availability-zone)
 *         ]
 *     end
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::auto-scaling-group auto-scaling-group-example
 *         auto-scaling-group-name: "auto-scaling-group-beam-1"
 *         launch-template-id: $(aws::launch-template launch-template-auto-scaling-group-example | launch-template-id)
 *         availability-zones: [
 *             $(aws::subnet subnet-auto-scaling-group-example | availability-zone),
 *             $(aws::subnet subnet-auto-scaling-group-example-2 | availability-zone)
 *         ]
 *     end
 */
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

    /**
     * The name of the auto scaling group, also served as its identifier and thus unique. (Required)
     */
    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public void setAutoScalingGroupName(String autoScalingGroupName) {
        this.autoScalingGroupName = autoScalingGroupName;
    }

    /**
     * The ID of an launched template that would be used as a skeleton to create the Auto scaling group.
     * Required if launch configuration name not provided.
     */
    @ResourceDiffProperty(updatable = true)
    public String getLaunchTemplateId() {
        return launchTemplateId;
    }

    public void setLaunchTemplateId(String launchTemplateId) {
        this.launchTemplateId = launchTemplateId;
    }

    /**
     *  A list of availability zones for the auto scale group to be active in. See `Distributing Instances Across Availability Zones <https://docs.aws.amazon.com/autoscaling/ec2/userguide/auto-scaling-benefits.html#arch-AutoScalingMultiAZ/>`_. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getAvailabilityZones() {
        if (availabilityZones == null) {
            availabilityZones = new ArrayList<>();
        }
        return availabilityZones;
    }

    public void setAvailabilityZones(List<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    /**
     * The maximum number of instances for the Auto Scaling group. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getMaxSize() {
        if (maxSize == null) {
            maxSize = 0;
        }
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * The minimum number of instances for the Auto Scaling group. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getMinSize() {
        if (minSize == null) {
            minSize = 0;
        }
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    /**
     * The desired number of instances for the Auto Scaling group. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getDesiredCapacity() {
        if (desiredCapacity == null) {
            desiredCapacity = 0;
        }
        return desiredCapacity;
    }

    public void setDesiredCapacity(Integer desiredCapacity) {
        this.desiredCapacity = desiredCapacity;
    }

    /**
     * The default cool down period in sec for the auto scale group. Defaults to 300 sec. See `Default Cool downs <https://docs.aws.amazon.com/autoscaling/ec2/userguide/Cooldown.html#cooldown-default/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getDefaultCooldown() {
        if (defaultCooldown == null) {
            defaultCooldown = 300;
        }
        return defaultCooldown;
    }

    public void setDefaultCooldown(Integer defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }

    /**
     * The type of health check to be performed on the auto scale group. Defaults to EC2. Can be 'EC2' or 'ELB'. See `Health Checks for Auto Scaling Instances <https://docs.aws.amazon.com/autoscaling/ec2/userguide/healthcheck.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckType() {
        if (healthCheckType == null) {
            healthCheckType = "EC2";
        }
        return healthCheckType.toUpperCase();
    }

    public void setHealthCheckType(String healthCheckType) {
        this.healthCheckType = healthCheckType;
    }

    /**
     * The grace period after which health check is started, to give time for the instances to start up. Defaults to 0 sec. See `Health Checks for Auto Scaling Instances <https://docs.aws.amazon.com/autoscaling/ec2/userguide/healthcheck.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHealthCheckGracePeriod() {
        if (healthCheckGracePeriod == null) {
            healthCheckGracePeriod = 0;
        }
        return healthCheckGracePeriod;
    }

    public void setHealthCheckGracePeriod(Integer healthCheckGracePeriod) {
        this.healthCheckGracePeriod = healthCheckGracePeriod;
    }

    /**
     * The name of a launched configuration that would be used as a skeleton to create the Auto scaling group.
     * Required if launch template Id is not provided.
     */
    @ResourceDiffProperty(updatable = true)
    public String getLaunchConfigurationName() {
        return launchConfigurationName;
    }

    public void setLaunchConfigurationName(String launchConfigurationName) {
        this.launchConfigurationName = launchConfigurationName;
    }

    /**
     * Enable protection of instances from auto scale group scale in. Defaults to false. see `Controlling Which Auto Scaling Instances Terminate During Scale In <https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-instance-termination.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getNewInstancesProtectedFromScaleIn() {
        if (newInstancesProtectedFromScaleIn == null) {
            newInstancesProtectedFromScaleIn = false;
        }
        return newInstancesProtectedFromScaleIn;
    }

    public void setNewInstancesProtectedFromScaleIn(Boolean newInstancesProtectedFromScaleIn) {
        this.newInstancesProtectedFromScaleIn = newInstancesProtectedFromScaleIn;
    }

    /**
     * A list of subnet identifiers. If Availability Zone is provided, subnet's need to be part of that. See `Launching Auto Scaling Instances in a VPC <https://docs.aws.amazon.com/autoscaling/ec2/userguide/asg-in-vpc.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
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
        setSubnetIds(autoScalingGroup.vpcZoneIdentifier().equals("")
            ? new ArrayList<>() : Arrays.asList(autoScalingGroup.vpcZoneIdentifier().split(",")));

        return true;
    }

    @Override
    public void create() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();

        client.createAutoScalingGroup(
            o -> o.autoScalingGroupName(getAutoScalingGroupName())
                .maxSize(getMaxSize())
                .minSize(getMinSize())
                .availabilityZones(getAvailabilityZones())
                .desiredCapacity(getDesiredCapacity())
                .defaultCooldown(getDefaultCooldown())
                .healthCheckType(getHealthCheckType())
                .healthCheckGracePeriod(getHealthCheckGracePeriod())
                .launchConfigurationName(getLaunchConfigurationName())
                .newInstancesProtectedFromScaleIn(getNewInstancesProtectedFromScaleIn())
                .vpcZoneIdentifier(getSubnetIds().isEmpty() ? " " : StringUtils.join(getSubnetIds(), ","))
                .launchTemplate(LaunchTemplateSpecification.builder().launchTemplateId(getLaunchTemplateId()).build())
        );

        AutoScalingGroup autoScalingGroup = getAutoScalingGroup(client);

        if (autoScalingGroup != null) {
            setArn(autoScalingGroup.autoScalingGroupARN());
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();

        client.updateAutoScalingGroup(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
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
                .vpcZoneIdentifier(getSubnetIds().isEmpty() ? " " : StringUtils.join(getSubnetIds(), ","))
        );
    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        // have option of graceful delete with configurable timeouts.
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
        if (ObjectUtils.isBlank(getLaunchTemplateId()) && ObjectUtils.isBlank(getLaunchConfigurationName())) {
            throw new BeamException("Either Launch template id or a launch configuration name is required.");
        }

        if (!getHealthCheckType().equals("ELB") && !getHealthCheckType().equals("EC2")) {
            throw new BeamException("The value - (" + getHealthCheckType()
                + ") is invalid for parameter Health Check Type.");
        }

        if (getHealthCheckGracePeriod() < 0) {
            throw new BeamException("The value - (" + getHealthCheckGracePeriod()
                + ") is invalid for parameter Health Check Grace period. Integer value grater or equal to 0.");
        }

        if (getMaxSize() < 0) {
            throw new BeamException("The value - (" + getMaxSize()
                + ") is invalid for parameter Max size. Integer value grater or equal to 0.");
        }

        if (getMinSize() < 0) {
            throw new BeamException("The value - (" + getMinSize()
                + ") is invalid for parameter Min size. Integer value grater or equal to 0.");
        }

        if (getDefaultCooldown() < 0) {
            throw new BeamException("The value - (" + getDefaultCooldown()
                + ") is invalid for parameter Default cool down. Integer value grater or equal to 0.");
        }

        if (getDesiredCapacity() < 0) {
            throw new BeamException("The value - (" + getDesiredCapacity()
                + ") is invalid for parameter Desired capacity. Integer value grater or equal to 0.");
        }
    }
}
