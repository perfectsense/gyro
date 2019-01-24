package beam.aws.cloudwatch;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricDataQueryResource {

    private String id;
    private String expression;
    private String label;
    private Boolean returnData;
    private String metricName;
    private String namespace;
    private String stat;
    private String unit;
    private Integer period;
    private Map<String, String> dimensions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getReturnData() {
        if (returnData == null) {
            returnData = false;
        }

        return returnData;
    }

    public void setReturnData(Boolean returnData) {
        this.returnData = returnData;
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

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
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

    public MetricDataQueryResource() {

    }

    public MetricDataQueryResource(MetricDataQuery metricDataQuery) {
        setId(metricDataQuery.id());
        setExpression(metricDataQuery.expression());
        setReturnData(metricDataQuery.returnData());
        setLabel(metricDataQuery.label());

        setMetricName(metricDataQuery.metricStat().metric().metricName());
        setNamespace(metricDataQuery.metricStat().metric().namespace());

        setPeriod(metricDataQuery.metricStat().period());
        setStat(metricDataQuery.metricStat().stat());
        setUnit(metricDataQuery.metricStat().unitAsString());

        for (Dimension dimension : metricDataQuery.metricStat().metric().dimensions()) {
            getDimensions().put(dimension.name(), dimension.value());
        }
    }

    public MetricDataQuery getMetricDataQuery() {
        return MetricDataQuery.builder()
            .expression(getExpression())
            .id(getId())
            .label(getLabel())
            .metricStat(
                m -> m.metric(
                    mm -> mm.namespace(getNamespace())
                    .metricName(getMetricName())
                    .dimensions(getDimensions().entrySet().stream().map(d -> Dimension.builder()
                        .name(d.getKey()).value(d.getValue()).build()).collect(Collectors.toList()))
                )
                .period(getPeriod())
                .stat(getStat())
                .unit(getUnit())
            )
            .build();
    }
}
