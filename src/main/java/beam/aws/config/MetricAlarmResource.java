package beam.aws.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;

import beam.diff.ResourceDiffProperty;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;

public class MetricAlarmResource extends AWSResource<MetricAlarm> {

    private Boolean actionsEnabled;
    private Set<String> alarmActions;
    private String alarmDescription;
    private String alarmName;
    private String comparisonOperator;
    private Map<String, String> dimensions;
    private Integer evaluationPeriods;
    private Set<String> insufficientDataActions;
    private String metricName;
    private String namespace;
    private Set<String> okActions;
    private Integer period;
    private String statistic;
    private Double threshold;
    private String unit;

    public Boolean getActionsEnabled() {
        return actionsEnabled;
    }

    public void setActionsEnabled(Boolean actionsEnabled) {
        this.actionsEnabled = actionsEnabled;
    }

    public Set<String> getAlarmActions() {
        AutoScalingGroupPolicyResource parentPolicy = findParent(AutoScalingGroupPolicyResource.class);

        if (parentPolicy != null) {
            return Collections.singleton(parentPolicy.getPolicyARN());

        } else {
            if (alarmActions == null) {
                alarmActions = new HashSet<>();
            }

            return alarmActions;
        }
    }

    public void setAlarmActions(Set<String> alarmActions) {
        this.alarmActions = alarmActions;
    }

    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    @ResourceDiffProperty(updatable = true)
    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public Map<String, String> getDimensions() {
        if (dimensions == null) {
            dimensions = new HashMap<>();
        }
        return dimensions;
    }

    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getEvaluationPeriods() {
        return evaluationPeriods;
    }

    public void setEvaluationPeriods(Integer evaluationPeriods) {
        this.evaluationPeriods = evaluationPeriods;
    }

    public Set<String> getInsufficientDataActions() {
        if (insufficientDataActions == null) {
            insufficientDataActions = new HashSet<>();
        }
        return insufficientDataActions;
    }

    public void setInsufficientDataActions(Set<String> insufficientDataActions) {
        this.insufficientDataActions = insufficientDataActions;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Set<String> getOKActions() {
        if (okActions == null) {
            okActions = new HashSet<>();
        }
        return okActions;
    }

    public void setOKActions(Set<String> okActions) {
        this.okActions = okActions;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getStatistic() {
        return statistic;
    }

    public void setStatistic(String statistic) {
        this.statistic = statistic;
    }

    @ResourceDiffProperty(updatable = true)
    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getAlarmName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, MetricAlarm alarm) {
        setActionsEnabled(alarm.getActionsEnabled());
        setAlarmActions(new HashSet<>(alarm.getAlarmActions()));
        setAlarmDescription(alarm.getAlarmDescription());
        setAlarmName(alarm.getAlarmName());
        setComparisonOperator(alarm.getComparisonOperator());

        for (Dimension d : alarm.getDimensions()) {
            getDimensions().put(d.getName(), d.getValue());
        }

        setEvaluationPeriods(alarm.getEvaluationPeriods());
        setInsufficientDataActions(new HashSet<>(alarm.getInsufficientDataActions()));
        setMetricName(alarm.getMetricName());
        setNamespace(alarm.getNamespace());
        setOKActions(new HashSet<>(alarm.getOKActions()));
        setPeriod(alarm.getPeriod());
        setStatistic(alarm.getStatistic());
        setThreshold(alarm.getThreshold());
        setUnit(alarm.getUnit());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonCloudWatchClient client = createClient(AmazonCloudWatchClient.class, cloud.getProvider());
        PutMetricAlarmRequest pmaRequest = new PutMetricAlarmRequest();

        pmaRequest.setActionsEnabled(getActionsEnabled());
        pmaRequest.setAlarmActions(getAlarmActions());
        pmaRequest.setAlarmDescription(getAlarmDescription());
        pmaRequest.setAlarmName(getAlarmName());
        pmaRequest.setComparisonOperator(getComparisonOperator());

        for (Map.Entry<String, String> entry : getDimensions().entrySet()) {
            pmaRequest.getDimensions().add(new Dimension().
                    withName(entry.getKey()).
                    withValue(entry.getValue()));
        }

        pmaRequest.setEvaluationPeriods(getEvaluationPeriods());
        pmaRequest.setInsufficientDataActions(getInsufficientDataActions());
        pmaRequest.setMetricName(getMetricName());
        pmaRequest.setNamespace(getNamespace());
        pmaRequest.setOKActions(getOKActions());
        pmaRequest.setPeriod(getPeriod());
        pmaRequest.setStatistic(getStatistic());
        pmaRequest.setThreshold(getThreshold());
        pmaRequest.setUnit(getUnit());
        client.putMetricAlarm(pmaRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, MetricAlarm> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonCloudWatchClient client = createClient(AmazonCloudWatchClient.class, cloud.getProvider());
        DeleteAlarmsRequest daRequest = new DeleteAlarmsRequest();

        daRequest.setAlarmNames(Arrays.asList(getAlarmName()));
        client.deleteAlarms(daRequest);
    }

    @Override
    public String toDisplayString() {
        return "metric alarm " + getAlarmName();
    }
}
