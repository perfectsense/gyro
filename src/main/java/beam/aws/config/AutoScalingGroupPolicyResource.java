package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;

public class AutoScalingGroupPolicyResource extends AWSResource<ScalingPolicy> {

    private BeamReference autoScalingGroup;
    private String adjustmentType;
    private Integer cooldown;
    private MetricAlarmResource metricAlarm;
    private Integer minAdjustmentStep;
    private String policyARN;
    private String policyName;
    private Integer scalingAdjustment;

    public BeamReference getAutoScalingGroup() {
        return newParentReference(AutoScalingGroupResource.class, autoScalingGroup);
    }

    public void setAutoScalingGroup(BeamReference autoScalingGroup) {
        this.autoScalingGroup = autoScalingGroup;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getCooldown() {
        return cooldown;
    }

    public void setCooldown(Integer cooldown) {
        this.cooldown = cooldown;
    }

    public MetricAlarmResource getMetricAlarm() {
        return metricAlarm;
    }

    public void setMetricAlarm(MetricAlarmResource metricAlarm) {
        this.metricAlarm = metricAlarm;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMinAdjustmentStep() {
        return minAdjustmentStep;
    }

    public void setMinAdjustmentStep(Integer minAdjustmentStep) {
        this.minAdjustmentStep = minAdjustmentStep;
    }

    public String getPolicyARN() {
        return policyARN;
    }

    public void setPolicyARN(String policyARN) {
        this.policyARN = policyARN;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getAutoScalingGroup(), getPolicyName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, ScalingPolicy policy) {
        setAdjustmentType(policy.getAdjustmentType());
        setCooldown(policy.getCooldown());
        setMinAdjustmentStep(policy.getMinAdjustmentStep());
        setPolicyARN(policy.getPolicyARN());
        setPolicyName(policy.getPolicyName());
        setScalingAdjustment(policy.getScalingAdjustment());

        // Alarm.
        AmazonCloudWatchClient client = createClient(AmazonCloudWatchClient.class, cloud.getProvider());
        DescribeAlarmsRequest daRequest = new DescribeAlarmsRequest();

        daRequest.setAlarmNames(Arrays.asList(getPolicyName()));

        for (MetricAlarm alarm : client.
                describeAlarms(daRequest).
                getMetricAlarms()) {

            if (isInclude(filter, alarm)) {
                MetricAlarmResource alarmResource = new MetricAlarmResource();
                alarmResource.setRegion(getRegion());

                alarmResource.init(cloud, filter, alarm);
                setMetricAlarm(alarmResource);
                break;
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.createOne(getMetricAlarm());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, ScalingPolicy> current) throws Exception {
        AutoScalingGroupPolicyResource currentPolicy = (AutoScalingGroupPolicyResource) current;

        update.updateOne(currentPolicy.getMetricAlarm(), getMetricAlarm());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.deleteOne(getMetricAlarm());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        PutScalingPolicyRequest pspRequest = new PutScalingPolicyRequest();

        pspRequest.setAdjustmentType(getAdjustmentType());
        pspRequest.setAutoScalingGroupName(getAutoScalingGroup().awsId());
        pspRequest.setCooldown(getCooldown());
        pspRequest.setMinAdjustmentStep(getMinAdjustmentStep());
        pspRequest.setPolicyName(getPolicyName());
        pspRequest.setScalingAdjustment(getScalingAdjustment());
        setPolicyARN(client.putScalingPolicy(pspRequest).getPolicyARN());
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, ScalingPolicy> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        DeletePolicyRequest dpRequest = new DeletePolicyRequest();

        dpRequest.setAutoScalingGroupName(getAutoScalingGroup().awsId());
        dpRequest.setPolicyName(getPolicyName());
        client.deletePolicy(dpRequest);
    }

    @Override
    public String toDisplayString() {
        return "auto scaling group policy " + getPolicyName();
    }
}
