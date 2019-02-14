package beam.aws.autoscaling;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.PutScalingPolicyResponse;
import software.amazon.awssdk.services.autoscaling.model.ScalingPolicy;
import software.amazon.awssdk.services.autoscaling.model.StepAdjustment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName(parent = "auto-scaling-group", value = "scaling-policy")
public class AutoScalingPolicyResource extends AwsResource {

    private String policyName;
    private String autoScalingGroupName;
    private String adjustmentType;
    private Integer cooldown;
    private Integer estimatedInstanceWarmup;
    private String metricAggregationType;
    private Integer minAdjustmentMagnitude;
    private String policyType;
    private Integer scalingAdjustment;
    private Boolean disableScaleIn;
    private Double targetValue;
    private String predefinedMetricType;
    private String predefinedMetricResourceLabel;
    private String policyArn;
    private List<AutoScalingPolicyStepAdjustment> stepAdjustment;

    /**
     * The name of the policy. (Required)
     */
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
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
     * The adjustment type. Valid values [ 'ChangeInCapacity', 'ExactCapacity', 'PercentChangeInCapacity' ].
     */
    @ResourceDiffProperty(updatable = true)
    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    /**
     * The amount of time between two scaling events. Valid values [ Integer greater than 0 ].
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getCooldown() {
        return cooldown;
    }

    public void setCooldown(Integer cooldown) {
        this.cooldown = cooldown;
    }

    /**
     * The estimated time, in seconds, until a newly launched instance can contribute to the CloudWatch metrics.
     * Valid values [ Integer greater than 0 ].
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getEstimatedInstanceWarmup() {
        return estimatedInstanceWarmup;
    }

    public void setEstimatedInstanceWarmup(Integer estimatedInstanceWarmup) {
        this.estimatedInstanceWarmup = estimatedInstanceWarmup;
    }

    /**
     * the aggregation type for cloud watch metrics. Valid values [ 'Minimum', 'Maximum', 'Average' ].
     */
    @ResourceDiffProperty(updatable = true)
    public String getMetricAggregationType() {
        if (metricAggregationType == null) {
            metricAggregationType = "Average";
        }

        return metricAggregationType;
    }

    public void setMetricAggregationType(String metricAggregationType) {
        this.metricAggregationType = metricAggregationType;
    }

    /**
     * The minimum number of instances to scale.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getMinAdjustmentMagnitude() {
        return minAdjustmentMagnitude;
    }

    public void setMinAdjustmentMagnitude(Integer minAdjustmentMagnitude) {
        this.minAdjustmentMagnitude = minAdjustmentMagnitude;
    }

    /**
     * The type of policy. Defaults to 'SimpleScaling'. Valid values [ 'SimpleScaling', 'StepScaling', 'TargetTrackingScaling' ]. (Required)
     */
    public String getPolicyType() {
        if (policyType == null) {
            policyType = "SimpleScaling";
        }
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    /**
     * The amount by which the scaling would happen.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    /**
     * Scaling in by the target tracking policy is enabled/disabled. Defaulted to false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDisableScaleIn() {
        if (disableScaleIn == null) {
            disableScaleIn = false;
        }

        return disableScaleIn;
    }

    public void setDisableScaleIn(Boolean disableScaleIn) {
        this.disableScaleIn = disableScaleIn;
    }

    /**
     * The target value for the metric.
     */
    @ResourceDiffProperty(updatable = true)
    public Double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
    }

    /**
     * Predefined metric type.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPredefinedMetricType() {
        return predefinedMetricType;
    }

    public void setPredefinedMetricType(String predefinedMetricType) {
        this.predefinedMetricType = predefinedMetricType;
    }

    /**
     * Predefined defines metric resource label.
     *
     * Valid values are ``ASGAverageCPUUtilization``, ``ASGAverageNetworkIn``, ``ASGAverageNetworkOut``, ``ALBRequestCountPerTarget``.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPredefinedMetricResourceLabel() {
        return predefinedMetricResourceLabel;
    }

    public void setPredefinedMetricResourceLabel(String predefinedMetricResourceLabel) {
        this.predefinedMetricResourceLabel = predefinedMetricResourceLabel;
    }

    public String getPolicyArn() {
        return policyArn;
    }

    public void setPolicyArn(String policyArn) {
        this.policyArn = policyArn;
    }

    /**
     * A set of adjustments that enable you to scale based on the size of the alarm breach.
     */
    @ResourceDiffProperty(updatable = true)
    public List<AutoScalingPolicyStepAdjustment> getStepAdjustment() {
        if (stepAdjustment == null) {
            stepAdjustment = new ArrayList<>();
        }
        return stepAdjustment;
    }

    public void setStepAdjustment(List<AutoScalingPolicyStepAdjustment> stepAdjustment) {
        this.stepAdjustment = stepAdjustment;
    }

    public AutoScalingPolicyResource() {

    }

    public AutoScalingPolicyResource(ScalingPolicy scalingPolicy) {
        setAdjustmentType(scalingPolicy.adjustmentType());
        setPolicyName(scalingPolicy.policyName());
        setAutoScalingGroupName(scalingPolicy.autoScalingGroupName());
        setCooldown(scalingPolicy.cooldown());
        setEstimatedInstanceWarmup(scalingPolicy.estimatedInstanceWarmup());
        setMetricAggregationType(scalingPolicy.metricAggregationType());
        setMinAdjustmentMagnitude(scalingPolicy.minAdjustmentMagnitude());
        setPolicyType(scalingPolicy.policyType());
        setScalingAdjustment(scalingPolicy.scalingAdjustment());
        setPolicyArn(scalingPolicy.policyARN());

        if (scalingPolicy.stepAdjustments() != null && !scalingPolicy.stepAdjustments().isEmpty()) {
            for (StepAdjustment stepAdjustment : scalingPolicy.stepAdjustments()) {
                AutoScalingPolicyStepAdjustment policyStepAdjustment = new AutoScalingPolicyStepAdjustment();
                policyStepAdjustment.setScalingAdjustment(stepAdjustment.scalingAdjustment());
                policyStepAdjustment.setMetricIntervalLowerBound(stepAdjustment.metricIntervalLowerBound());
                policyStepAdjustment.setMetricIntervalUpperBound(stepAdjustment.metricIntervalUpperBound());
                getStepAdjustment().add(policyStepAdjustment);
            }
        }

        if (scalingPolicy.targetTrackingConfiguration() != null) {
            setDisableScaleIn(scalingPolicy.targetTrackingConfiguration().disableScaleIn());
            setTargetValue(scalingPolicy.targetTrackingConfiguration().targetValue());

            if (scalingPolicy.targetTrackingConfiguration().predefinedMetricSpecification() != null) {
                setPredefinedMetricType(scalingPolicy.targetTrackingConfiguration().predefinedMetricSpecification().predefinedMetricTypeAsString());
                setPredefinedMetricResourceLabel(scalingPolicy.targetTrackingConfiguration().predefinedMetricSpecification().resourceLabel());
            }
        }
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
        savePolicy(client);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        validate();
        savePolicy(client);
    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.deletePolicy(
            r -> r.autoScalingGroupName(getAutoScalingGroupName())
                .policyName(getPolicyName())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("auto scaling policy");

        if (!ObjectUtils.isBlank(getPolicyName())) {
            sb.append(" - ").append(getPolicyName());
        }

        if (!ObjectUtils.isBlank(getPolicyType())) {
            sb.append(" [ ").append(getPolicyType()).append(" ] ");
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s %s", getPolicyName(), getPolicyType());
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }

    private void savePolicy(AutoScalingClient client) {
        PutScalingPolicyResponse response = null;

        if (getPolicyType().equalsIgnoreCase("SimpleScaling")) {
            response = client.putScalingPolicy(
                r -> r.policyName(getPolicyName())
                    .autoScalingGroupName(getAutoScalingGroupName())
                    .adjustmentType(getAdjustmentType())
                    .policyType(getPolicyType())
                    .cooldown(getCooldown())
                    .scalingAdjustment(getScalingAdjustment())
                    .minAdjustmentMagnitude(getMinAdjustmentMagnitude())
            );
        } else if (getPolicyType().equalsIgnoreCase("StepScaling")) {
            response = client.putScalingPolicy(
                r -> r.policyName(getPolicyName())
                    .autoScalingGroupName(getAutoScalingGroupName())
                    .adjustmentType(getAdjustmentType())
                    .policyType(getPolicyType())
                    .estimatedInstanceWarmup(getEstimatedInstanceWarmup())
                    .metricAggregationType(getMetricAggregationType())
                    .stepAdjustments(stepAdjustment.stream().map(AutoScalingPolicyStepAdjustment::getStepPolicyStep).collect(Collectors.toList()))
            );
        } else if (getPolicyType().equalsIgnoreCase("TargetTrackingScaling")) {
            response = client.putScalingPolicy(
                r -> r.policyName(getPolicyName())
                    .autoScalingGroupName(getAutoScalingGroupName())
                    .policyType(getPolicyType())
                    .estimatedInstanceWarmup(getEstimatedInstanceWarmup())
                    .targetTrackingConfiguration(
                        t -> t.targetValue(getTargetValue())
                            .predefinedMetricSpecification(
                                p -> p.predefinedMetricType(getPredefinedMetricType())
                                    .resourceLabel(getPredefinedMetricResourceLabel())
                            )
                            .disableScaleIn(getDisableScaleIn())
                    )
            );
        }

        if (response != null) {
            setPolicyArn(response.policyARN());
        }
    }

    private String getParentId() {
        AutoScalingGroupResource parent = (AutoScalingGroupResource) parentResource();
        if (parent == null) {
            throw new BeamException("Parent Auto Scale Group resource not found.");
        }
        return parent.getAutoScalingGroupName();
    }

    private void validate() {
        // policy type validation
        if (!getPolicyType().equals("SimpleScaling")
            && !getPolicyType().equals("StepScaling")
            && !getPolicyType().equals("TargetTrackingScaling")) {
            throw new BeamException("Invalid value '" + getPolicyType() + "' for the param 'policy-type'."
                + " Valid options ['SimpleScaling', 'StepScaling', 'TargetTrackingScaling'].");
        }

        // Attribute validation when not SimpleScaling
        if (!getPolicyType().equals("SimpleScaling")) {
            if (getCooldown() != null) {
                throw new BeamException("The param 'cooldown' is only allowed when"
                    + " 'policy-type' is 'SimpleScaling'.");
            }

            if (getScalingAdjustment() != null) {
                throw new BeamException("The param 'scaling-adjustment' is only allowed when"
                    + " 'policy-type' is 'SimpleScaling'.");
            }
        }

        // Attribute validation when not StepScaling
        if (!getPolicyType().equals("StepScaling")) {
            if (getMetricAggregationType() != null && !getMetricAggregationType().equalsIgnoreCase("average")) {
                throw new BeamException("The param 'metric-aggregation-type' is only allowed when"
                    + " 'policy-type' is 'StepScaling'.");
            }

            if (!getStepAdjustment().isEmpty()) {
                throw new BeamException("The param 'step-adjustment' is only allowed when"
                    + " 'policy-type' is 'StepScaling'.");
            }
        }

        // Attribute validation when not TargetTrackingScaling
        if (!getPolicyType().equals("TargetTrackingScaling")) {
            if (getTargetValue() != null) {
                throw new BeamException("The param 'target-value' is only allowed when"
                    + " 'policy-type' is 'TargetTrackingScaling'.");
            }

            if (getPredefinedMetricType() != null) {
                throw new BeamException("The param 'predefined-metric-type' is only allowed when"
                    + " 'policy-type' is 'TargetTrackingScaling'.");
            }

            if (getPredefinedMetricResourceLabel() != null) {
                throw new BeamException("The param 'predefined-metric-resource-label' is only allowed when"
                    + " 'policy-type' is 'TargetTrackingScaling'.");
            }

            if (getDisableScaleIn()) {
                throw new BeamException("The param 'disable-scale-in' is only allowed when"
                    + " 'policy-type' is 'TargetTrackingScaling'.");
            }

            // when simple or step
            if (getAdjustmentType() == null
                || (getAdjustmentType().equals("ChangeInCapacity")
                && getAdjustmentType().equals("ExactCapacity")
                && getAdjustmentType().equals("PercentChangeInCapacity"))) {
                throw new BeamException("Invalid value '" + getAdjustmentType() + "' for the param 'adjustment-type'."
                    + " Valid options ['ChangeInCapacity', 'ExactCapacity', 'PercentChangeInCapacity'].");
            } else if (!getAdjustmentType().equals("PercentChangeInCapacity") && getMinAdjustmentMagnitude() != null) {
                throw new BeamException("The param 'min-adjustment-magnitude' is only allowed when 'adjustment-type' is 'PercentChangeInCapacity'.");
            }
        }

        // Attribute validation when SimpleScaling
        if (getPolicyType().equals("SimpleScaling")) {
            if (getEstimatedInstanceWarmup() != null) {
                throw new BeamException("The param 'estimated-instance-warmup' is only allowed when"
                    + " 'policy-type' is either 'StepScaling' or 'TargetTrackingScaling'.");
            }

            if (getCooldown() < 0) {
                throw new BeamException("Invalid value '" + getCooldown() + "' for the param 'cooldown'. Needs to be a positive integer.");
            }
        }

        // Attribute validation when StepScaling
        if (getPolicyType().equals("StepScaling")) {
            if (getStepAdjustment().isEmpty()) {
                throw new BeamException("the param 'step-adjustment' needs to have at least one step.");
            }

            if (!getMetricAggregationType().equals("Minimum")
                && !getMetricAggregationType().equals("Maximum")
                && !getMetricAggregationType().equals("Average")) {
                throw new BeamException("Invalid value '" + getMetricAggregationType() + "' for the param 'metric-aggregation-type'."
                    + " Valid options ['Minimum', 'Maximum', 'Average'].");
            }
        }

        // Attribute validation when TargetTrackingScaling
        if (getPolicyType().equals("TargetTrackingScaling")) {
            if (getMinAdjustmentMagnitude() != null) {
                throw new BeamException("The param 'min-adjustment-magnitude' is only allowed when"
                    + " 'policy-type' is either 'StepScaling' or 'SimpleScaling'.");
            }

            if (getAdjustmentType() != null) {
                throw new BeamException("The param 'adjustment-type' is only allowed when"
                    + " 'policy-type' is either 'StepScaling' or 'SimpleScaling'.");
            }
        }
    }
}
