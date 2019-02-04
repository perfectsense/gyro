package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.HealthCheck;
import software.amazon.awssdk.services.route53.model.HealthCheckConfig;

import java.util.List;
import java.util.Set;

@ResourceName("health-check")
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
    private List<String> regions;
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

    @ResourceDiffProperty(updatable = true)
    public List<String> getChildHealthChecks() {
        return childHealthChecks;
    }

    public void setChildHealthChecks(List<String> childHealthChecks) {
        this.childHealthChecks = childHealthChecks;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableSni() {
        return enableSni;
    }

    public void setEnableSni(Boolean enableSni) {
        this.enableSni = enableSni;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    @ResourceDiffProperty(updatable = true)
    public String getFullyQualifiedDomainName() {
        return fullyQualifiedDomainName;
    }

    public void setFullyQualifiedDomainName(String fullyQualifiedDomainName) {
        this.fullyQualifiedDomainName = fullyQualifiedDomainName;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getHealthThreshold() {
        return healthThreshold;
    }

    public void setHealthThreshold(Integer healthThreshold) {
        this.healthThreshold = healthThreshold;
    }

    @ResourceDiffProperty(updatable = true)
    public String getInsufficientDataHealthStatus() {
        return insufficientDataHealthStatus;
    }

    public void setInsufficientDataHealthStatus(String insufficientDataHealthStatus) {
        this.insufficientDataHealthStatus = insufficientDataHealthStatus;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getInverted() {
        return inverted;
    }

    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }

    @ResourceDiffProperty(updatable = true)
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

    @ResourceDiffProperty(updatable = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public Integer getRequestInterval() {
        return requestInterval;
    }

    public void setRequestInterval(Integer requestInterval) {
        this.requestInterval = requestInterval;
    }

    @ResourceDiffProperty(updatable = true)
    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @ResourceDiffProperty(updatable = true)
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

    @ResourceDiffProperty(updatable = true)
    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    @ResourceDiffProperty(updatable = true)
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
        healthCheck.cloudWatchAlarmConfiguration();
        healthCheck.linkedService();
        HealthCheckConfig healthCheckConfig = healthCheck.healthCheckConfig();
        setChildHealthChecks(healthCheckConfig.childHealthChecks());
        setDisabled(healthCheckConfig.disabled());
        setEnableSni(healthCheckConfig.enableSNI());
        setFailureThreshold(healthCheckConfig.failureThreshold());
        setFullyQualifiedDomainName(healthCheckConfig.fullyQualifiedDomainName());
        setHealthThreshold(healthCheckConfig.healthThreshold());
        setInsufficientDataHealthStatus(healthCheckConfig.insufficientDataHealthStatusAsString());
        setInverted(healthCheckConfig.inverted());
        setIpAddress(healthCheckConfig.ipAddress());
        setMeasureLatency(healthCheckConfig.measureLatency());
        setPort(healthCheckConfig.port());
        setRegions(healthCheckConfig.regionsAsStrings());
        setRequestInterval(healthCheckConfig.requestInterval());
        setResourcePath(healthCheckConfig.resourcePath());
        setSearchString(healthCheckConfig.searchString());
        setType(healthCheckConfig.typeAsString());

        if (healthCheckConfig.alarmIdentifier() != null) {
            setAlarmName(healthCheckConfig.alarmIdentifier().name());
            setAlarmName(healthCheckConfig.alarmIdentifier().regionAsString());
        }

        return true;
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
                    .regionsWithStrings(getRegions())
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
                .regionsWithStrings(getRegions())
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
        StringBuilder sb = new StringBuilder();

        sb.append("health check");

        if (!ObjectUtils.isBlank(getHealthCheckId())) {
            sb.append(" - ").append(getHealthCheckId());
        }

        return sb.toString();
    }
}
