package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.ChangeTagsForResourceRequest;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.HealthCheck;
import software.amazon.awssdk.services.route53.model.HealthCheckConfig;
import software.amazon.awssdk.services.route53.model.HealthCheckType;
import software.amazon.awssdk.services.route53.model.InsufficientDataHealthStatus;
import software.amazon.awssdk.services.route53.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.route53.model.Tag;
import software.amazon.awssdk.services.route53.model.TagResourceType;
import software.amazon.awssdk.services.route53.model.UpdateHealthCheckRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Map<String, String> tags;

    public String getHealthCheckId() {
        return healthCheckId;
    }

    public void setHealthCheckId(String healthCheckId) {
        this.healthCheckId = healthCheckId;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getChildHealthChecks() {
        if (childHealthChecks == null) {
            childHealthChecks = new ArrayList<>();
        }

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
        return type != null ? type.toUpperCase() : null;
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

    @ResourceDiffProperty(updatable = true, nullable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean refresh() {

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

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
            setAlarmRegion(healthCheckConfig.alarmIdentifier().regionAsString());
        }

        loadTags(client);

        return true;
    }

    @Override
    public void create() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

        CreateHealthCheckResponse response = client.createHealthCheck(
            r -> r.callerReference(UUID.randomUUID().toString())
                .healthCheckConfig(getCreateHealthCheckRequest())
        );

        HealthCheck healthCheck = response.healthCheck();
        setHealthCheckId(healthCheck.id());
        saveTags(client, new HashMap<>());

        /*setHealthCheckId("a998c083-4560-4a8d-8a3c-5df57967219d");
        refresh();

        throw new BeamException("Testing");*/
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        validate();

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

        if (changedProperties.contains("tags")) {
            HealthCheckResource oldResource = (HealthCheckResource) current;
            saveTags(client, oldResource.getTags());
        }

        client.updateHealthCheck(getUpdateHealthCheckRequest());
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

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

        if (!ObjectUtils.isBlank(getType())) {
            sb.append(" [ ").append(getType()).append(" ]");
        }

        return sb.toString();
    }

    private HealthCheckConfig getCreateHealthCheckRequest() {
        if (getType().equals("CALCULATED")) {
            return HealthCheckConfig.builder()
                .type(getType())
                .childHealthChecks(getChildHealthChecks())
                .disabled(getDisabled())
                .inverted(getInverted())
                .healthThreshold(getHealthThreshold())
                .build();

        } else if (getType().equals("CLOUDWATCH_METRIC")) {
            return HealthCheckConfig.builder()
                .type(getType())
                .disabled(getDisabled())
                .insufficientDataHealthStatus(getInsufficientDataHealthStatus())
                .inverted(getInverted())
                .alarmIdentifier(
                    a -> a.name(getAlarmName())
                        .region(getAlarmRegion())
                )
                .build();

        } else {
            return HealthCheckConfig.builder()
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
                .build();
        }
    }

    private UpdateHealthCheckRequest getUpdateHealthCheckRequest() {
        if (getType().equals("CALCULATED")) {
            return UpdateHealthCheckRequest.builder()
                .healthCheckId(getHealthCheckId())
                .childHealthChecks(getChildHealthChecks())
                .disabled(getDisabled())
                .inverted(getInverted())
                .healthThreshold(getHealthThreshold())
                .build();
        } else if (getType().equals("CLOUDWATCH_METRIC")) {
            return UpdateHealthCheckRequest.builder()
                .healthCheckId(getHealthCheckId())
                .disabled(getDisabled())
                .insufficientDataHealthStatus(getInsufficientDataHealthStatus())
                .inverted(getInverted())
                .alarmIdentifier(
                    a -> a.name(getAlarmName())
                        .region(getAlarmRegion())
                )
                .build();
        } else {
            return UpdateHealthCheckRequest.builder()
                .healthCheckId(getHealthCheckId())

                .build();
        }
    }

    private List<Tag> getRoute53Tags(Map<String, String> tags) {
        List<Tag> tagList = new ArrayList<>();

        for (String key: tags.keySet()) {
            tagList.add(
                Tag.builder()
                    .key(key)
                    .value(tags.get(key))
                    .build()
            );
        }

        return tagList;
    }

    private void saveTags(Route53Client client, Map<String, String> oldTags) {
        if (!oldTags.isEmpty() || !getTags().isEmpty()) {
            MapDifference<String, String> diff = Maps.difference(oldTags, getTags());

            ChangeTagsForResourceRequest tagRequest;

            if (getTags().isEmpty()) {
                tagRequest = ChangeTagsForResourceRequest.builder()
                    .resourceId(getHealthCheckId())
                    .resourceType(TagResourceType.HEALTHCHECK)
                    .removeTagKeys(diff.entriesOnlyOnLeft().keySet())
                    .build();
            } else if (diff.entriesOnlyOnLeft().isEmpty()) {
                tagRequest = ChangeTagsForResourceRequest.builder()
                    .resourceId(getHealthCheckId())
                    .resourceType(TagResourceType.HEALTHCHECK)
                    .addTags(getRoute53Tags(getTags()))
                    .build();
            } else {
                tagRequest = ChangeTagsForResourceRequest.builder()
                    .resourceId(getHealthCheckId())
                    .resourceType(TagResourceType.HEALTHCHECK)
                    .addTags(getRoute53Tags(getTags()))
                    .removeTagKeys(diff.entriesOnlyOnLeft().keySet())
                    .build();
            }

            client.changeTagsForResource(tagRequest);
        }
    }

    private void loadTags(Route53Client client) {
        ListTagsForResourceResponse response = client.listTagsForResource(
            r -> r.resourceId(getHealthCheckId())
                .resourceType(TagResourceType.HEALTHCHECK)
        );

        List<Tag> tags = response.resourceTagSet().tags();

        getTags().clear();

        for (Tag tag : tags) {
            getTags().put(tag.key(), tag.value());
        }
    }

    private void validate() {
        //Type validation
        if (ObjectUtils.isBlank(getType())
            || HealthCheckType.fromValue(getType()).equals(HealthCheckType.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException(String.format("Invalid value '%s' for param 'type'. Valid values [ '%s' ]", getType(),
                Stream.of(HealthCheckType.values())
                    .filter(o -> !o.equals(HealthCheckType.UNKNOWN_TO_SDK_VERSION))
                    .map(Enum::toString).collect(Collectors.joining("', '"))));
        }

        //Attribute validation when type not CALCULATED
        if (!getType().equals("CALCULATED")) {
            if (!ObjectUtils.isBlank(getHealthThreshold())) {
                throw new BeamException("The param 'health-threshold' is only allowed when"
                    + " 'type' is 'CALCULATED'.");
            }

            if (!getChildHealthChecks().isEmpty()) {
                throw new BeamException("The param 'child-health-checks' is only allowed when"
                    + " 'type' is 'CALCULATED'.");
            }
        }

        //Attribute validation when type not CLOUDWATCH_METRIC
        if (!getType().equals("CLOUDWATCH_METRIC")) {
            if (!ObjectUtils.isBlank(getInsufficientDataHealthStatus())) {
                throw new BeamException("The param 'insufficient-data-health-status' is only allowed when"
                    + " 'type' is 'CLOUDWATCH_METRIC'.");
            }

            if (!ObjectUtils.isBlank(getAlarmName())) {
                throw new BeamException("The param 'alarm-name' is only allowed when"
                    + " 'type' is 'CLOUDWATCH_METRIC'.");
            }

            if (!ObjectUtils.isBlank(getAlarmRegion())) {
                throw new BeamException("The param 'alarm-region' is only allowed when"
                    + " 'type' is 'CLOUDWATCH_METRIC'.");
            }
        }

        //Attribute validation when type CALCULATED
        if (getType().equals("CALCULATED")) {
            if (ObjectUtils.isBlank(getHealthThreshold()) || getHealthThreshold() < 0) {
                throw new BeamException("The value - (" + getHealthThreshold()
                    + ") is invalid for parameter Health Check Grace period. Integer value grater or equal to 0.");
            }
        }

        //Attribute validation when type CLOUDWATCH_METRIC
        if (getType().equals("CLOUDWATCH_METRIC")) {
            if (ObjectUtils.isBlank(getInsufficientDataHealthStatus())
                || InsufficientDataHealthStatus.fromValue(getInsufficientDataHealthStatus())
                .equals(InsufficientDataHealthStatus.UNKNOWN_TO_SDK_VERSION)) {
                throw new BeamException(String.format("Invalid value '%s' for param 'insufficient-data-health-status'."
                        + " Valid values [ '%s' ]", getInsufficientDataHealthStatus(),
                    Stream.of(InsufficientDataHealthStatus.values())
                        .filter(o -> !o.equals(InsufficientDataHealthStatus.UNKNOWN_TO_SDK_VERSION))
                        .map(Enum::toString).collect(Collectors.joining("', '"))));
            }
        }
    }
}
