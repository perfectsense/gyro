package beam.aws.route53;

import beam.aws.AwsResource;
import beam.lang.Resource;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.HealthCheck;

import java.util.List;
import java.util.Set;

public class HealthCheckResource extends AwsResource {

    private String healthCheckId;
    private List<String> childHealthChecks;
    private Boolean disabled;
    private Boolean enableSni;
    private Integer failureThreshold;
    private String fullyQualifiedDomainName;
    private Integer healthThreshold;
    private String insufficientDataHealthStatus;
    private Boolean inverted;
    private String ipAddress;
    private Boolean measureLatency;
    private Integer port;
    private List<String> regionsWithStrings;
    private Integer requestInterval;
    private String resourcePath;
    private String searchString;
    private String type;
    private String alarmName;
    private String alarmRegion;

    public String getHealthCheckId() {
        return healthCheckId;
    }

    public void setHealthCheckId(String healthCheckId) {
        this.healthCheckId = healthCheckId;
    }

    public List<String> getChildHealthChecks() {
        return childHealthChecks;
    }

    public void setChildHealthChecks(List<String> childHealthChecks) {
        this.childHealthChecks = childHealthChecks;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getEnableSni() {
        return enableSni;
    }

    public void setEnableSni(Boolean enableSni) {
        this.enableSni = enableSni;
    }

    public Integer getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public String getFullyQualifiedDomainName() {
        return fullyQualifiedDomainName;
    }

    public void setFullyQualifiedDomainName(String fullyQualifiedDomainName) {
        this.fullyQualifiedDomainName = fullyQualifiedDomainName;
    }

    public Integer getHealthThreshold() {
        return healthThreshold;
    }

    public void setHealthThreshold(Integer healthThreshold) {
        this.healthThreshold = healthThreshold;
    }

    public String getInsufficientDataHealthStatus() {
        return insufficientDataHealthStatus;
    }

    public void setInsufficientDataHealthStatus(String insufficientDataHealthStatus) {
        this.insufficientDataHealthStatus = insufficientDataHealthStatus;
    }

    public Boolean getInverted() {
        return inverted;
    }

    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getMeasureLatency() {
        return measureLatency;
    }

    public void setMeasureLatency(Boolean measureLatency) {
        this.measureLatency = measureLatency;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public List<String> getRegionsWithStrings() {
        return regionsWithStrings;
    }

    public void setRegionsWithStrings(List<String> regionsWithStrings) {
        this.regionsWithStrings = regionsWithStrings;
    }

    public Integer getRequestInterval() {
        return requestInterval;
    }

    public void setRequestInterval(Integer requestInterval) {
        this.requestInterval = requestInterval;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public String getAlarmRegion() {
        return alarmRegion;
    }

    public void setAlarmRegion(String alarmRegion) {
        this.alarmRegion = alarmRegion;
    }

    @Override
    public boolean refresh() {

        Route53Client client = createClient(Route53Client.class);

        GetHealthCheckResponse response = client.getHealthCheck(
            r -> r.healthCheckId(getHealthCheckId())
        );

        HealthCheck healthCheck = response.healthCheck();
        healthCheck.id();
        healthCheck.cloudWatchAlarmConfiguration();
        healthCheck.healthCheckConfig();

        return false;
    }

    @Override
    public void create() {
        Route53Client client = createClient(Route53Client.class);

        CreateHealthCheckResponse response = client.createHealthCheck(
            r -> r.healthCheckConfig(
                h -> h.childHealthChecks(getChildHealthChecks())
                    .disabled(getDisabled())
                    .enableSNI(getEnableSni())
                    .failureThreshold(getFailureThreshold())
                    .fullyQualifiedDomainName(getFullyQualifiedDomainName())
                    .healthThreshold(getHealthThreshold())
                    .insufficientDataHealthStatus(getInsufficientDataHealthStatus())
                    .inverted(getInverted())
                    .ipAddress(getIpAddress())
                    .measureLatency(getMeasureLatency())
                    .port(getPort())
                    .regionsWithStrings(getRegionsWithStrings())
                    .requestInterval(getRequestInterval())
                    .resourcePath(getResourcePath())
                    .searchString(getSearchString())
                    .type(getType())
                    .alarmIdentifier(
                        a -> a.name(getAlarmName())
                            .region(getAlarmRegion())
                    )
            )
        );

        HealthCheck healthCheck = response.healthCheck();
        setHealthCheckId(healthCheck.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Route53Client client = createClient(Route53Client.class);

        client.updateHealthCheck(
            r -> r.healthCheckId(getHealthCheckId())
                .childHealthChecks(getChildHealthChecks())
                .disabled(getDisabled())
                .enableSNI(getEnableSni())
                .failureThreshold(getFailureThreshold())
                .fullyQualifiedDomainName(getFullyQualifiedDomainName())
                .healthThreshold(getHealthThreshold())
                .insufficientDataHealthStatus(getInsufficientDataHealthStatus())
                .inverted(getInverted())
                .ipAddress(getIpAddress())
                .port(getPort())
                .regionsWithStrings(getRegionsWithStrings())
                .resourcePath(getResourcePath())
                .searchString(getSearchString())
                .alarmIdentifier(
                    a -> a.name(getAlarmName())
                        .region(getAlarmRegion())
                )
        );
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class);

        client.deleteHealthCheck(
            r -> r.healthCheckId(getHealthCheckId())
        );
    }

    @Override
    public String toDisplayString() {
        return null;
    }
}
