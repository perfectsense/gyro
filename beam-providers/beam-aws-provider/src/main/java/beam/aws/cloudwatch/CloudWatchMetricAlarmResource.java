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


/**
 * Creates an Auto scaling Group from a Launch Configuration or from a Launch Template.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::metric-alarm metric-alarm-example
 *         alarm-name: "metric-alarm-example"
 *         alarm-description: "metric-alarm-example-update"
 *         comparison-operator: "GreaterThanOrEqualToThreshold"
 *         threshold: 0.1
 *         evaluation-periods: 1
 *         metric-name: "CPUUtilization"
 *         period: 60
 *         namespace: "AWS/EC2"
 *         statistic: "SampleCount"
 *     end
 */
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

    /**
     * The name of the alarm. (Required)
     */
    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    /**
     * Indicates if actions are to be executed when reaches the alarm state. Defaults to true.
     */
    public Boolean getActionsEnabled() {
        if (actionsEnabled == null) {
            actionsEnabled = true;
        }

        return actionsEnabled;
    }

    public void setActionsEnabled(Boolean actionsEnabled) {
        this.actionsEnabled = actionsEnabled;
    }

    /**
     * A set of actions to be executed when this alarm transitions to the ALARM state. Each action is a resource ARN.
     */
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

    /**
     * A description for the alarm.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    /**
     * The operation to use when comparing using threshold and statistics. Valid values [ GreaterThanOrEqualToThreshold, GreaterThanThreshold, LessThanThreshold, LessThanOrEqualToThreshold ]
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    /**
     * Number of data points to breach to trigger this alarm. Valid values [ Integer greater than 0 ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getDatapointsToAlarm() {
        return datapointsToAlarm;
    }

    public void setDatapointsToAlarm(Integer datapointsToAlarm) {
        this.datapointsToAlarm = datapointsToAlarm;
    }

    /**
     * Key Value pair to specify what the metric is as specified in the metric name. Max limit of 10.
     */
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

    /**
     * This value indicates if less data points are present to evaluate a trigger, what should be done. 'ignore' would ignore the data at that point. Valid values [ evaluate, ignore ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getEvaluateLowSampleCountPercentile() {
        return evaluateLowSampleCountPercentile;
    }

    public void setEvaluateLowSampleCountPercentile(String evaluateLowSampleCountPercentile) {
        this.evaluateLowSampleCountPercentile = evaluateLowSampleCountPercentile;
    }

    /**
     * The number of period over which the data point's are evaluated. Valid values [ Integer greater than 0 ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getEvaluationPeriods() {
        return evaluationPeriods;
    }

    public void setEvaluationPeriods(Integer evaluationPeriods) {
        this.evaluationPeriods = evaluationPeriods;
    }

    /**
     * The percentile statistic for the metric specified in MetricName. Valid values [ Between p0.0 and p100 ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getExtendedStatistic() {
        return extendedStatistic;
    }

    public void setExtendedStatistic(String extendedStatistic) {
        this.extendedStatistic = extendedStatistic;
    }

    /**
     * A set of actions to execute when this alarm transitions to the INSUFFICIENT_DATA state. ach action is a resource ARN.
     */
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

    /**
     * The name of the metric associated with the alarm. See `AWS Services That Publish CloudWatch Metrics <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/aws-services-cloudwatch-metrics.html/>`_.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * The namespace associated with the metric specified in 'metric-name' provided.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * A set of actions to execute when this alarm transitions to the OK state. ach action is a resource ARN.
     */
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

    /**
     * The length, in seconds, used each time the metric specified in 'metric-name' is evaluated.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    /**
     * The namespace associated with the metric specified in 'metric-name' provided. Valid values [ SampleCount, Average, Sum, Minimum, Maximum ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getStatistic() {
        return statistic;
    }

    public void setStatistic(String statistic) {
        this.statistic = statistic;
    }

    /**
     * The value against which the specified statistic is compared.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    /**
     * How the metric handles missing data. Defaults to 'missing'. Valid values [ breaching, notBreaching, ignore, missing ].
     */
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

    /**
     * The unit of measure for the statistic.
     */
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
