package beam.aws.cloudwatch;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("metric-alarm")
public class CloudWatchMetricAlarmResource extends AwsResource {

    private String alarmName;
    private Boolean actionsEnabled;
    private List<String> alarmActions;
    private String alarmDescription;
    private String comparisonOperator;
    private Integer datapointsToAlarm;
    private Map<String, String> dimensions;
    private String evaluateLowSampleCountPercentile;
    private Integer evaluationPeriods;
    private String extendedStatistic;
    private List<String> insufficientDataActions;
    private String metricName;
    private String namespace;
    private List<String> okActions;
    private Integer period;
    private String statistic;
    private Double threshold;
    private String treatMissingData;
    private String unit;
    private String arn;
    private List<MetricDataQueryResource> metrics;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public Boolean getActionsEnabled() {
        return actionsEnabled;
    }

    public void setActionsEnabled(Boolean actionsEnabled) {
        this.actionsEnabled = actionsEnabled;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getAlarmActions() {
        if (alarmActions == null) {
            alarmActions = new ArrayList<>();
        }

        return alarmActions;
    }

    public void setAlarmActions(List<String> alarmActions) {
        this.alarmActions = alarmActions;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getDatapointsToAlarm() {
        return datapointsToAlarm;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public void setDatapointsToAlarm(Integer datapointsToAlarm) {
        this.datapointsToAlarm = datapointsToAlarm;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Map<String, String> getDimensions() {
        if (dimensions == null) {
            dimensions = new HashMap<>();
        }

        return dimensions;
    }

    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getEvaluateLowSampleCountPercentile() {
        return evaluateLowSampleCountPercentile;
    }

    public void setEvaluateLowSampleCountPercentile(String evaluateLowSampleCountPercentile) {
        this.evaluateLowSampleCountPercentile = evaluateLowSampleCountPercentile;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getEvaluationPeriods() {
        return evaluationPeriods;
    }

    public void setEvaluationPeriods(Integer evaluationPeriods) {
        this.evaluationPeriods = evaluationPeriods;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getExtendedStatistic() {
        return extendedStatistic;
    }

    public void setExtendedStatistic(String extendedStatistic) {
        this.extendedStatistic = extendedStatistic;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getInsufficientDataActions() {
        if (insufficientDataActions == null) {
            insufficientDataActions = new ArrayList<>();
        }

        return insufficientDataActions;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public void setInsufficientDataActions(List<String> insufficientDataActions) {
        this.insufficientDataActions = insufficientDataActions;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getOkActions() {
        if (okActions == null) {
            okActions = new ArrayList<>();
        }

        return okActions;
    }

    public void setOkActions(List<String> okActions) {
        this.okActions = okActions;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getStatistic() {
        return statistic;
    }

    public void setStatistic(String statistic) {
        this.statistic = statistic;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTreatMissingData() {
        if (treatMissingData == null) {
            treatMissingData = "missing";
        }

        return treatMissingData;
    }

    public void setTreatMissingData(String treatMissingData) {
        this.treatMissingData = treatMissingData;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<MetricDataQueryResource> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricDataQueryResource> metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean refresh() {
        CloudWatchClient client = createClient(CloudWatchClient.class);

        MetricAlarm metricAlarm = getMetricAlarm(client);

        if (metricAlarm == null) {
            return false;
        }

        setActionsEnabled(metricAlarm.actionsEnabled());
        setAlarmActions(metricAlarm.alarmActions());
        setAlarmDescription(metricAlarm.alarmDescription());
        setComparisonOperator(metricAlarm.comparisonOperator() != null ? metricAlarm.comparisonOperator().toString() : null);
        setDatapointsToAlarm(metricAlarm.datapointsToAlarm());
        setEvaluateLowSampleCountPercentile(metricAlarm.evaluateLowSampleCountPercentile());
        setEvaluationPeriods(metricAlarm.evaluationPeriods());
        setExtendedStatistic(metricAlarm.extendedStatistic());
        setInsufficientDataActions(metricAlarm.insufficientDataActions());
        setMetricName(metricAlarm.metricName());
        setNamespace(metricAlarm.namespace());
        setOkActions(metricAlarm.okActions());
        setPeriod(metricAlarm.period());
        setStatistic(metricAlarm.statistic() != null ? metricAlarm.statistic().toString() : null);
        setThreshold(metricAlarm.threshold());
        setTreatMissingData(metricAlarm.treatMissingData());
        setUnit(metricAlarm.unit() != null ? metricAlarm.unit().toString() : null);
        setArn(metricAlarm.alarmArn());

        for (Dimension dimension : metricAlarm.dimensions()) {
            getDimensions().put(dimension.name(), dimension.value());
        }

        setMetrics(new ArrayList<>());
        for (software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery metricDataQuery : metricAlarm.metrics()) {
            MetricDataQueryResource metricDataQueryResource = new MetricDataQueryResource(metricDataQuery);
            getMetrics().add(metricDataQueryResource);
        }

        return true;
    }

    @Override
    public void create() {
        CloudWatchClient client = createClient(CloudWatchClient.class);

        saveMetricAlarm(client);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        CloudWatchClient client = createClient(CloudWatchClient.class);

        saveMetricAlarm(client);
    }

    @Override
    public void delete() {
        CloudWatchClient client = createClient(CloudWatchClient.class);

        client.deleteAlarms(r -> r.alarmNames(Collections.singleton(getAlarmName())));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("metric alarm");

        if (!ObjectUtils.isBlank(getAlarmName())) {
            sb.append(" - ").append(getAlarmName());
        }

        return sb.toString();
    }

    private MetricAlarm getMetricAlarm(CloudWatchClient client) {
        if (ObjectUtils.isBlank(getAlarmName())) {
            throw new BeamException("alarm-name is missing, unable to load metric alarm.");
        }

        try {
            DescribeAlarmsResponse response = client.describeAlarms(r -> r.alarmNames(Collections.singleton(getAlarmName())));

            if (response.metricAlarms().isEmpty()) {
                return null;
            }

            return response.metricAlarms().get(0);
        } catch (CloudWatchException ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return null;
            }

            throw ex;
        }
    }

    private void saveMetricAlarm(CloudWatchClient client) {
        client.putMetricAlarm(
            r -> r.alarmName(getAlarmName())
                .actionsEnabled(getActionsEnabled())
                .alarmActions(getAlarmActions())
                .alarmDescription(getAlarmDescription())
                .comparisonOperator(getComparisonOperator())
                .datapointsToAlarm(getDatapointsToAlarm())
                .evaluateLowSampleCountPercentile(getEvaluateLowSampleCountPercentile())
                .evaluationPeriods(getEvaluationPeriods())
                .extendedStatistic(getExtendedStatistic())
                .insufficientDataActions(getInsufficientDataActions())
                .metricName(getMetricName())
                .namespace(getNamespace())
                .okActions(getOkActions())
                .period(getPeriod())
                .statistic(getStatistic())
                .threshold(getThreshold())
                .treatMissingData(getTreatMissingData())
                .unit(getUnit())
                .dimensions(getDimensions().entrySet().stream().map(m -> Dimension.builder()
                    .name(m.getKey()).value(m.getValue()).build()).collect(Collectors.toList()))

                .metrics(getMetrics() != null
                    ? getMetrics().stream()
                    .map(MetricDataQueryResource::getMetricDataQuery)
                    .collect(Collectors.toList()) : null)
        );
    }
}
