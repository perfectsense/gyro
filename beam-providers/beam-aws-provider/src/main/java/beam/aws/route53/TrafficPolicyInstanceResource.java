package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateTrafficPolicyInstanceResponse;
import software.amazon.awssdk.services.route53.model.GetTrafficPolicyInstanceResponse;
import software.amazon.awssdk.services.route53.model.TrafficPolicyInstance;

import java.util.Set;

@ResourceName("traffic-policy-instance")
public class TrafficPolicyInstanceResource extends AwsResource {
    private String name;
    private String message;
    private String hostedZoneId;
    private String trafficPolicyId;
    private String type;
    private Long ttl;
    private String state;
    private Integer version;
    private String trafficPolicyInstanceId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTrafficPolicyId() {
        return trafficPolicyId;
    }

    public void setTrafficPolicyId(String trafficPolicyId) {
        this.trafficPolicyId = trafficPolicyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTrafficPolicyInstanceId() {
        return trafficPolicyInstanceId;
    }

    public void setTrafficPolicyInstanceId(String trafficPolicyInstanceId) {
        this.trafficPolicyInstanceId = trafficPolicyInstanceId;
    }

    @Override
    public boolean refresh() {
        Route53Client client = createClient(Route53Client.class);

        GetTrafficPolicyInstanceResponse response = client.getTrafficPolicyInstance(
            r -> r.id(getTrafficPolicyInstanceId())
        );

        TrafficPolicyInstance trafficPolicyInstance = response.trafficPolicyInstance();
        setName(trafficPolicyInstance.name());
        setMessage(trafficPolicyInstance.message());
        setHostedZoneId(trafficPolicyInstance.hostedZoneId());
        setType(trafficPolicyInstance.trafficPolicyTypeAsString());
        setTtl(trafficPolicyInstance.ttl());
        setState(trafficPolicyInstance.state());
        setVersion(trafficPolicyInstance.trafficPolicyVersion());
        setTrafficPolicyId(trafficPolicyInstance.trafficPolicyId());

        return true;
    }

    @Override
    public void create() {
        Route53Client client = createClient(Route53Client.class);

        CreateTrafficPolicyInstanceResponse response = client.createTrafficPolicyInstance(
            r -> r.name(getName())
                .hostedZoneId(getHostedZoneId())
                .trafficPolicyId(getTrafficPolicyId())
                .trafficPolicyVersion(getVersion())
                .ttl(getTtl())
        );

        TrafficPolicyInstance trafficPolicyInstance = response.trafficPolicyInstance();
        setTrafficPolicyInstanceId(trafficPolicyInstance.id());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Route53Client client = createClient(Route53Client.class);

        client.updateTrafficPolicyInstance(
            r -> r.id(getTrafficPolicyInstanceId())
                .trafficPolicyId(getTrafficPolicyId())
                .trafficPolicyVersion(getVersion())
                .ttl(getTtl())
        );
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class);

        client.deleteTrafficPolicyInstance(
            r -> r.id(getTrafficPolicyInstanceId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("traffic policy instance");

        if (!ObjectUtils.isBlank(getTrafficPolicyInstanceId())) {
            sb.append(" - ").append(getTrafficPolicyInstanceId());

        }

        return sb.toString();
    }
}
