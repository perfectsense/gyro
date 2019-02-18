package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.google.common.collect.ImmutableSet;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates a record set in the given hosted zone.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::record-set record-set-example
 *         hosted-zone-name: $(aws::hosted-zone hosted-zone-record-set-example-alpha | hosted-zone-name)
 *         hosted-zone-id: $(aws::hosted-zone hosted-zone-record-set-example-alpha | hosted-zone-id)
 *         name: "record-set-example."
 *         type: "A"
 *         ttl: 300
 *         records: [
 *             "192.0.2.235",
 *             "192.0.2.236"
 *         ]
 *         failover: "secondary"
 *         set-identifier: "set_id"
 *         routing-policy: "failover"
 *         health-check-id: $(aws::health-check health-check-record-set-example-calculated-alpha | health-check-id)
 *     end
 */
@ResourceName("record-set")
public class RecordSetResource extends AwsResource {
    private String comment;
    private String continentCode;
    private String countryCode;
    private String dnsName;
    private Boolean evaluateTargetHealth;
    private String failover;
    private String hostedZoneId;
    private String healthCheckId;
    private Boolean multiValueAnswer;
    private String name;
    private String region;
    private String setIdentifier;
    private String subdivisionCode;
    private String trafficPolicyInstanceId;
    private Long ttl;
    private String type;
    private Long weight;
    private List<String> records;
    private String routingPolicy;
    private Boolean enableAlias;
    private String aliasHostedZoneId;

    private static final Set<String> RoutingPolicySet = ImmutableSet.of("geolocation","failover","multivalue","weighted","latency","simple");

    /**
     * A comment when creating/updating/deleting a record set.
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * The continent code. At least one of continent code, country code or subdivision code required if type selected as 'geolocation'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getContinentCode() {
        return continentCode != null ? continentCode.toUpperCase() : null;
    }

    public void setContinentCode(String continentCode) {
        this.continentCode = continentCode;
    }

    /**
     * The country code. At least one of continent code, country code or subdivision code required if 'type' selected as 'geolocation'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getCountryCode() {
        return countryCode != null ? countryCode.toUpperCase() : null;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Dns name to associate with this record set. Required if 'enable alias' is set to 'true'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getDnsName() {
        if (dnsName != null) {
            dnsName += dnsName.endsWith(".") ? "" : ".";
        }

        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    /**
     * Enable target health evaluation with this record set. Required if 'enable alias' is set to 'true'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Boolean getEvaluateTargetHealth() {
        return evaluateTargetHealth;
    }

    public void setEvaluateTargetHealth(Boolean evaluateTargetHealth) {
        this.evaluateTargetHealth = evaluateTargetHealth;
    }

    /**
     * The failover value. Valid values [ Primary, Secondary]. Required if 'route policy' set to 'failover'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getFailover() {
        return failover != null ? failover.toUpperCase() : null;
    }

    public void setFailover(String failover) {
        this.failover = failover;
    }

    /**
     * The id of the hosted zone under which the the record set is to be created. (Required)
     */
    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    /**
     * The id of a health check to be associated with the record set. Required if 'failover' is set to 'primary'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getHealthCheckId() {
        return healthCheckId;
    }

    public void setHealthCheckId(String healthCheckId) {
        this.healthCheckId = healthCheckId;
    }

    /**
     * Needs to be enabled if routing policy is 'multivalue'. Required if 'route policy' set to 'multivalue'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Boolean getMultiValueAnswer() {
        return multiValueAnswer;
    }

    public void setMultiValueAnswer(Boolean multiValueAnswer) {
        this.multiValueAnswer = multiValueAnswer;
    }

    /**
     * The name of the record set being created. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The region where the records mentioned resides. Required if 'route policy' set to 'latency'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * a set identifier that differentiates this from other record set of the same type and routing policy. Required if 'enable alias' is set to 'false'.
     */
    @ResourceDiffProperty(updatable = true)
    public String getSetIdentifier() {
        return setIdentifier;
    }

    public void setSetIdentifier(String setIdentifier) {
        this.setIdentifier = setIdentifier;
    }

    /**
     * The sub division code. At least one of continent code, country code or subdivision code required if type selected as 'geolocation'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getSubdivisionCode() {
        return subdivisionCode != null ? subdivisionCode.toUpperCase() : null;
    }

    public void setSubdivisionCode(String subdivisionCode) {
        this.subdivisionCode = subdivisionCode;
    }

    /**
     * The id of a traffic policy instance to be associated with the record set.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getTrafficPolicyInstanceId() {
        return trafficPolicyInstanceId;
    }

    public void setTrafficPolicyInstanceId(String trafficPolicyInstanceId) {
        this.trafficPolicyInstanceId = trafficPolicyInstanceId;
    }

    /**
     * The resource record cache time to live. Valid values [ 0 - 172800]. Required if 'enable alias' is set to 'false'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    /**
     * The type of record set being created. Valid values [ SOA, A, TXT, NS, CNAME, MX, NAPTR, PTR, SRV, SPF, AAAA, CAA ]. (Required)
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * The weight value determines the probability of a record being selected. Valid values [ 0 - 255]. Required if 'route policy' set to 'weighted'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    /**
     * A list of ip addresses for the record set. Required if 'enable alias' is set to 'false'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getRecords() {
        if (records == null) {
            records = new ArrayList<>();
        }

        Collections.sort(records);
        return records;
    }

    public void setRecords(List<String> records) {
        this.records = records;
    }

    /**
     * Routing policy type the record set is going to be. Defaults to Simple. Valid Values [ 'geolocation', 'failover', 'multivalue', 'weighted', 'latency', 'simple' ].
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getRoutingPolicy() {
        if (routingPolicy == null) {
            routingPolicy = "simple";
        }

        return routingPolicy.toLowerCase();
    }

    public void setRoutingPolicy(String routingPolicy) {
        this.routingPolicy = routingPolicy;
    }

    /**
     * Enable alias. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Boolean getEnableAlias() {
        if (enableAlias == null) {
            enableAlias = false;
        }

        return enableAlias;
    }

    public void setEnableAlias(Boolean enableAlias) {
        this.enableAlias = enableAlias;
    }

    /**
     * The hosted zone where the 'dns name' belongs as configured. Required if 'enable alias' is set to 'true'.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getAliasHostedZoneId() {
        return aliasHostedZoneId;
    }

    public void setAliasHostedZoneId(String aliasHostedZoneId) {
        this.aliasHostedZoneId = aliasHostedZoneId;
    }

    @Override
    public boolean refresh() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        ResourceRecordSet recordSet = getResourceRecordSet(client);

        if (recordSet == null) {
            return false;
        }

        loadRecordSet(recordSet);

        return true;
    }

    @Override
    public void create() {
        validate();

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        if (getType().equals("NS") || getType().equals("SOA")) {
            saveResourceRecordSet(client,this, ChangeAction.UPSERT);
        } else {
            saveResourceRecordSet(client,this, ChangeAction.CREATE);
        }

        refresh();
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        validate();

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        RecordSetResource oldResource = (RecordSetResource) current;

        if (changedProperties.contains("name") || changedProperties.contains("set-identifier")) {
            saveResourceRecordSet(client, oldResource, ChangeAction.DELETE);
        }

        saveResourceRecordSet(client, this, ChangeAction.UPSERT);
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL.toString(), null);

        saveResourceRecordSet(client, this, ChangeAction.DELETE);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("record set").append(" - ");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(getName());
        }

        if (!ObjectUtils.isBlank(getType())) {
            sb.append(" type [ ").append(getType()).append(" ]");
        }

        if (!ObjectUtils.isBlank(getRoutingPolicy())) {
            sb.append(" routing policy [ ").append(getRoutingPolicy()).append(" ]");
        }

        return sb.toString();
    }

    private void loadRecordSet(ResourceRecordSet recordSet) {
        setFailover(recordSet.failoverAsString());
        setHealthCheckId(recordSet.healthCheckId());
        setMultiValueAnswer(recordSet.multiValueAnswer());
        setRegion(recordSet.regionAsString());
        setWeight(recordSet.weight());
        setTrafficPolicyInstanceId(recordSet.trafficPolicyInstanceId());
        setTtl(recordSet.ttl());
        setRecords(recordSet.resourceRecords().stream().map(ResourceRecord::value).collect(Collectors.toList()));

        if (recordSet.aliasTarget() != null) {
            setDnsName(recordSet.aliasTarget().dnsName());
            setEvaluateTargetHealth(recordSet.aliasTarget().evaluateTargetHealth());
            setAliasHostedZoneId(recordSet.aliasTarget().hostedZoneId());
        }

        if (recordSet.geoLocation() != null) {
            setCountryCode(recordSet.geoLocation().countryCode());
            setContinentCode(recordSet.geoLocation().continentCode());
            setSubdivisionCode(recordSet.geoLocation().subdivisionCode());
        }
    }

    private ResourceRecordSet getResourceRecordSet(Route53Client client) {
        ListResourceRecordSetsResponse response = client.listResourceRecordSets(
            r -> r.hostedZoneId(getHostedZoneId())
                .startRecordName(getName())
                .startRecordType(getType())
        );

        if (!response.resourceRecordSets().isEmpty()) {
            return response.resourceRecordSets().get(0);
        }

        return null;
    }

    private void saveResourceRecordSet(Route53Client client, RecordSetResource recordSetResource, ChangeAction changeAction) {
        ResourceRecordSet.Builder recordSetBuilder = ResourceRecordSet.builder()
            .name(recordSetResource.getName())
            .healthCheckId(recordSetResource.getHealthCheckId())
            .setIdentifier(recordSetResource.getSetIdentifier())
            .trafficPolicyInstanceId(recordSetResource.getTrafficPolicyInstanceId())
            .type(recordSetResource.getType());

        if (recordSetResource.getEnableAlias()) {
            recordSetBuilder.aliasTarget(
                a -> a.dnsName(recordSetResource.getDnsName())
                    .evaluateTargetHealth(recordSetResource.getEvaluateTargetHealth())
                    .hostedZoneId(recordSetResource.getAliasHostedZoneId()));
        } else {
            recordSetBuilder.resourceRecords(recordSetResource.getRecords().stream()
                .map(o -> ResourceRecord.builder().value(o).build())
                .collect(Collectors.toList()))
                .ttl(recordSetResource.getTtl());
        }

        switch (recordSetResource.getRoutingPolicy()) {
            case "geolocation":
                recordSetBuilder.geoLocation(
                    g -> g.continentCode(recordSetResource.getContinentCode())
                        .countryCode(recordSetResource.getCountryCode())
                        .subdivisionCode(recordSetResource.getSubdivisionCode()));
                break;
            case "failover":
                recordSetBuilder.failover(recordSetResource.getFailover());
                break;
            case "multivalue":
                recordSetBuilder.multiValueAnswer(recordSetResource.getMultiValueAnswer());
                break;
            case "weighted":
                recordSetBuilder.weight(recordSetResource.getWeight());
                break;
            case "latency":
                recordSetBuilder.region(recordSetResource.getRegion());
                break;
            default: break;
        }

        Change change = Change.builder()
            .action(changeAction)
            .resourceRecordSet(recordSetBuilder.build())
            .build();

        client.changeResourceRecordSets(
            r -> r.hostedZoneId(recordSetResource.getHostedZoneId())
                .changeBatch(
                    c -> c.comment(recordSetResource.getComment())
                        .changes(change)
                )
        );
    }

    private void validate() {
        if (! RoutingPolicySet.contains(getRoutingPolicy())) {
            throw new BeamException(String.format("The value - (%s) is invalid for parameter 'routing-policy'."
                + " Valid values [ '%s' ].",getRoutingPolicy(),String.join("', '",RoutingPolicySet)));
        }

        if (ObjectUtils.isBlank(getType())
            || RRType.fromValue(getType())
            .equals(RRType.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException(String.format("Invalid value '%s' for param 'insufficient-data-health-status'."
                    + " Valid values [ '%s' ]", getType(),
                Stream.of(RRType.values())
                    .filter(o -> !o.equals(RRType.UNKNOWN_TO_SDK_VERSION))
                    .map(Enum::toString).collect(Collectors.joining("', '"))));
        }

        if (getEnableAlias()) {
            if (!ObjectUtils.isBlank(getTtl())) {
                throw new BeamException("The param 'ttl' is not allowed when 'enable-alias' is set to 'true'.");
            }

            if (!getRecords().isEmpty()) {
                throw new BeamException("The param 'records' is not allowed when 'enable-alias' is set to 'true'.");
            }

            if (getEvaluateTargetHealth() == null) {
                throw new BeamException("The param 'evaluate-target-health' is required when 'enable-alias' is set to 'true'.");
            }

            if (ObjectUtils.isBlank(getDnsName())) {
                throw new BeamException("The param 'dns-name' is required when 'enable-alias' is set to 'true'.");
            }

            if (ObjectUtils.isBlank(getAliasHostedZoneId())) {
                throw new BeamException("The param 'alias-hosted-zone-id' is required when 'enable-alias' is set to 'true'.");
            }
        } else {
            if (getEvaluateTargetHealth() != null) {
                throw new BeamException("The param 'evaluate-target-health' is not allowed when 'enable-alias' is set to 'false' or not set.");
            }

            if (getDnsName() != null) {
                throw new BeamException("The param 'dns-name' is not allowed when 'enable-alias' is set to 'false' or not set.");
            }

            if (getAliasHostedZoneId() != null) {
                throw new BeamException("The param 'alias-hosted-zone-id' is not allowed when 'enable-alias' is set to 'false' or not set.");
            }

            if (ObjectUtils.isBlank(getTtl()) || getTtl() < 0 || getTtl() > 172800) {
                throw new BeamException("The param 'ttl' is required when 'enable-alias' is set to 'false' or not set."
                    + " Valid values [ Long 0 - 172800 ].");
            }

            if (getRecords().isEmpty()) {
                throw new BeamException("The param 'records' is required when 'enable-alias' is set to 'false' or not set.");
            }
        }

        if (!getRoutingPolicy().equals("geolocation")) {
            if (!ObjectUtils.isBlank(getContinentCode())) {
                throw new BeamException("The param 'continent-code' is not allowed when 'routing-policy' is not set to 'geolocation'.");
            }

            if (!ObjectUtils.isBlank(getCountryCode())) {
                throw new BeamException("The param 'country-code' is not allowed when 'routing-policy' is not set to 'geolocation'.");
            }

            if (!ObjectUtils.isBlank(getSubdivisionCode())) {
                throw new BeamException("The param 'subdivision-code' is not allowed when 'routing-policy' is not set to 'geolocation'.");
            }
        } else {
            if (ObjectUtils.isBlank(getContinentCode()) && ObjectUtils.isBlank(getCountryCode()) && ObjectUtils.isBlank(getSubdivisionCode())) {
                throw new BeamException("At least one of the param [ 'continent-code', 'country-code', 'subdivision-code']"
                    + " is required when 'routing-policy' is set to 'geolocation'.");
            }
        }

        if (!getRoutingPolicy().equals("failover") && getFailover() != null) {
            throw new BeamException("The param 'failover' is not allowed when 'routing-policy' is not set to 'failover'.");
        } else if (getRoutingPolicy().equals("failover")
            && (ObjectUtils.isBlank(getFailover()) || (!getFailover().equals("PRIMARY") && !getFailover().equals("SECONDARY")))) {
            throw new BeamException("The param 'failover' is required when 'routing-policy' is set to 'failover'."
                + " Valid values [ PRIMARY, SECONDARY ].");
        }

        if (!getRoutingPolicy().equals("multivalue") && getMultiValueAnswer() != null) {
            throw new BeamException("The param 'multi-value-answer' is not allowed when 'routing-policy' is not set to 'multivalue'.");
        } else if (getRoutingPolicy().equals("multivalue")) {
            if (getMultiValueAnswer() == null) {
                throw new BeamException("The param 'multi-value-answer' is required when 'routing-policy' is set to 'multivalue'.");
            }

            if (getRecords().size() > 1) {
                throw new BeamException("The param 'records' can only have one value when 'routing-policy' is set to 'multivalue'.");
            }
        }

        if (!getRoutingPolicy().equals("weighted") && getWeight() != null) {
            throw new BeamException("The param 'weight' is not allowed when 'routing-policy' is not set to 'weighted'.");
        } else if (getRoutingPolicy().equals("weighted")) {
            if ((getWeight() == null) || getWeight() < 0 || getWeight() > 255) {
                throw new BeamException("The param 'weight' is required when 'routing-policy' is set to 'weighted'."
                    + " Valid values [ Long 0 - 255 ].");
            }
        }

        if (!getRoutingPolicy().equals("latency") && getRegion() != null) {
            throw new BeamException("The param 'region' is not allowed when 'routing-policy' is not set to 'latency'.");
        } else if (getRoutingPolicy().equals("latency") && ObjectUtils.isBlank(getRegion())) {
            throw new BeamException("The param 'region' is required when 'routing-policy' is set to 'latency'.");
        }
    }
}
