package beam.aws.autoscaling;

import software.amazon.awssdk.services.autoscaling.model.StepAdjustment;

public class AutoScalingPolicyStepAdjustment {
    private Integer scalingAdjustment;
    private Double metricIntervalLowerBound;
    private Double metricIntervalUpperBound;

    public StepAdjustment getStepPolicyStep() {
        return StepAdjustment.builder()
            .scalingAdjustment(getScalingAdjustment())
            .metricIntervalLowerBound(getMetricIntervalLowerBound())
            .metricIntervalUpperBound(getMetricIntervalUpperBound())
            .build();
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public Double getMetricIntervalLowerBound() {
        return metricIntervalLowerBound;
    }

    public void setMetricIntervalLowerBound(Double metricIntervalLowerBound) {
        this.metricIntervalLowerBound = metricIntervalLowerBound;
    }

    public Double getMetricIntervalUpperBound() {
        return metricIntervalUpperBound;
    }

    public void setMetricIntervalUpperBound(Double metricIntervalUpperBound) {
        this.metricIntervalUpperBound = metricIntervalUpperBound;
    }
}
