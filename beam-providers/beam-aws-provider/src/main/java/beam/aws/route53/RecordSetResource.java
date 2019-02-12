package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName(parent = "hosted-zone", value = "record-set")
public class RecordSetResource extends AwsResource {
    private String comment;
    private String continentCode;
    private String countryCode;
    private String dnsName;
    private Boolean evaluateTargetHealth;
    private String failover;
    private String hostedZoneId;
    private String hostedZoneName;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ResourceDiffProperty(updatable = true)
    public String getContinentCode() {
        return continentCode;
    }

    public void setContinentCode(String continentCode) {
        this.continentCode = continentCode;
    }

    @ResourceDiffProperty(updatable = true)
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEvaluateTargetHealth() {
        return evaluateTargetHealth;
    }

    public void setEvaluateTargetHealth(Boolean evaluateTargetHealth) {
        this.evaluateTargetHealth = evaluateTargetHealth;
    }

    @ResourceDiffProperty(updatable = true)
    public String getFailover() {
        return failover;
    }

    public void setFailover(String failover) {
        this.failover = failover;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public String getHostedZoneName() {
        return hostedZoneName;
    }

    public void setHostedZoneName(String hostedZoneName) {
        this.hostedZoneName = hostedZoneName;
    }

    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckId() {
        return healthCheckId;
    }

    public void setHealthCheckId(String healthCheckId) {
        this.healthCheckId = healthCheckId;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getMultiValueAnswer() {
        return multiValueAnswer;
    }

    public void setMultiValueAnswer(Boolean multiValueAnswer) {
        this.multiValueAnswer = multiValueAnswer;
    }

    @ResourceDiffProperty(updatable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @ResourceDiffProperty(updatable = true)
    public String getSetIdentifier() {
        return setIdentifier;
    }

    public void setSetIdentifier(String setIdentifier) {
        this.setIdentifier = setIdentifier;
    }

    @ResourceDiffProperty(updatable = true)
    public String getSubdivisionCode() {
        return subdivisionCode;
    }

    public void setSubdivisionCode(String subdivisionCode) {
        this.subdivisionCode = subdivisionCode;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTrafficPolicyInstanceId() {
        return trafficPolicyInstanceId;
    }

    public void setTrafficPolicyInstanceId(String trafficPolicyInstanceId) {
        this.trafficPolicyInstanceId = trafficPolicyInstanceId;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    @ResourceDiffProperty(updatable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getRecords() {
        if (records == null) {
            records = new ArrayList<>();
        }

        return records;
    }

    public void setRecords(List<String> records) {
        this.records = records;
    }

    public RecordSetResource() {

    }

    public RecordSetResource(ResourceRecordSet recordSet, String hostedZoneName) {
        setFailover(recordSet.failoverAsString());
        setHealthCheckId(recordSet.healthCheckId());
        setMultiValueAnswer(recordSet.multiValueAnswer());
        setName(recordSet.name().replaceAll(hostedZoneName + "$",""));
        setRegion(recordSet.regionAsString());
        setWeight(recordSet.weight());
        setTrafficPolicyInstanceId(recordSet.trafficPolicyInstanceId());
        setTtl(recordSet.ttl());
        setType(recordSet.typeAsString());
        setRecords(recordSet.resourceRecords().stream().map(ResourceRecord::value).collect(Collectors.toList()));
        setHostedZoneName(hostedZoneName);

        if (recordSet.aliasTarget() != null) {
            setDnsName(recordSet.aliasTarget().dnsName());
            setEvaluateTargetHealth(recordSet.aliasTarget().evaluateTargetHealth());
            setHostedZoneId(recordSet.aliasTarget().hostedZoneId());
        }

        if (recordSet.geoLocation() != null) {
            setCountryCode(recordSet.geoLocation().countryCode());
            setContinentCode(recordSet.geoLocation().continentCode());
            setSubdivisionCode(recordSet.geoLocation().subdivisionCode());
        }
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        HostedZoneResource parent = (HostedZoneResource) parentResource();
        setHostedZoneName(parent.getHostedZoneName());
        setHostedZoneId(parent.getHostedZoneId());

        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

        Change change = Change.builder()
            .action(ChangeAction.CREATE)
            .resourceRecordSet(
                rr -> rr.name(getName()+getHostedZoneName())
                    .failover(getFailover())
                    .healthCheckId(getHealthCheckId())
                    .multiValueAnswer(getMultiValueAnswer())
                    .region(getRegion())
                    .setIdentifier(getSetIdentifier())
                    .trafficPolicyInstanceId(getTrafficPolicyInstanceId())
                    .ttl(getTtl())
                    .type(getType())
                    .weight(getWeight())
                    /*.aliasTarget(
                        a -> a.dnsName(getDnsName())
                            .evaluateTargetHealth(getEvaluateTargetHealth())
                            .hostedZoneId(getHostedZoneId()))*/
                    .resourceRecords(
                        getRecords().stream()
                            .map(o -> ResourceRecord.builder().value(o).build())
                            .collect(Collectors.toList()))
                    /*.geoLocation(g -> g
                        .continentCode(getContinentCode())
                        .countryCode(getCountryCode())
                        .subdivisionCode(getSubdivisionCode()))*/

            )
            .build();

        client.changeResourceRecordSets(
            r -> r.hostedZoneId(getHostedZoneId())
                .changeBatch(
                    c -> c.comment(getComment())
                        .changes(change)
                )
        );
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class, Region.AWS_GLOBAL);

        Change change = Change.builder()
            .action(ChangeAction.DELETE)
            .resourceRecordSet(rr -> rr.name(getName()))
            .build();

        client.changeResourceRecordSets(
            r -> r.hostedZoneId(getHostedZoneId())
                .changeBatch(
                    c -> c.comment(getComment())
                        .changes(change)
                )
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("record set").append(" - ");

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(getName());
        }

        if (!ObjectUtils.isBlank(getHostedZoneName())) {
            sb.append(getHostedZoneName());
        }

        if (!ObjectUtils.isBlank(getType())) {
            sb.append(" [ ").append(getType()).append(" ]");
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s %s", getName(), getType());
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }
}
