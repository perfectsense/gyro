package beam.aws.elbv2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import beam.lang.Resource;
import com.psddev.dari.util.CompactMap;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Matcher;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::target-group target-group-example
 *         target-group-name: "test-target-group"
 *         port: "80"
 *         protocol: "HTTP"
 *         target-type: "instance"
 *         vpc-id: $(aws::vpc vpc | vpc-id)
 *         health-check-enabled: "true"
 *
 *         health-check
 *             health-check-interval: "90"
 *             health-check-path: "/"
 *             health-check-port: "traffic-port"
 *             health-check-protocol: "HTTP"
 *             health-check-timeout: "30"
 *             healthy-threshold: "2"
 *             matcher: "200"
 *             unhealthy-threshold: "2"
 *         end
 *
 *         target
 *             id: $(aws::instance instance-us-east-2a | instance-id)
 *             port: "80"
 *         end
 *
 *         target
 *             id: $(aws::instance instance-us-east-2b | instance-id)
 *             port: "443"
 *         end
 *
 *         tags: {
 *                 Name: "alb-example-target-group"
 *             }
 *     end
 */

@ResourceName("target-group")
public class TargetGroupResource extends AwsResource {

    private HealthCheckResource healthCheck;
    private Boolean healthCheckEnabled;
    private Integer port;
    private String protocol;
    private Map<String, String> tags;
    private List<TargetResource> target;
    private String targetGroupArn;
    private String targetGroupName;
    private String targetType;
    private String vpcId;

    /**
     *  The health check associated with the target group (Optional)
     */
    public HealthCheckResource getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheckResource healthCheck) {
        this.healthCheck = healthCheck;
    }

    /**
     *  Indicates if health checks are enabled (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    /**
     *  Port on which traffic is received by targets (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     *  Protocol used to route traffic to targets (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     *  List of tags associated with the target group (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    /**
     *  List of targets associated with the target group (Optional)
     */
    @ResourceDiffProperty(subresource = true, nullable = true, updatable = true)
    public List<TargetResource> getTarget() {
        if (target == null) {
            target = new ArrayList<>();
        }

        return target;
    }

    public void setTarget(List<TargetResource> target) {
        this.target = target;
    }

    public String getTargetGroupArn() {
        return targetGroupArn;
    }

    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }

    public String getTargetGroupName() {
        return targetGroupName;
    }

    public void setTargetGroupName(String targetGroupName) {
        this.targetGroupName = targetGroupName;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public boolean refresh() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        DescribeTargetGroupsResponse tgResponse = client.describeTargetGroups(r -> r.targetGroupArns(getTargetGroupArn()));

        if (tgResponse != null) {
            TargetGroup tg = tgResponse.targetGroups().get(0);
            setHealthCheck(new HealthCheckResource(tg));
            setHealthCheckEnabled(tg.healthCheckEnabled());
            setPort(tg.port());
            setProtocol(tg.healthCheckProtocolAsString());
            setTargetGroupArn(tg.targetGroupArn());
            setTargetGroupName(tg.targetGroupName());
            setTargetType(tg.targetTypeAsString());
            setVpcId(tg.vpcId());

            DescribeTargetHealthResponse response = client.describeTargetHealth(r -> r.targets(toTargets())
                                                                                        .targetGroupArn(getTargetGroupArn()));
            getTarget().clear();
            for (TargetHealthDescription targetHealthDescription : response.targetHealthDescriptions()) {
                TargetHealth health = targetHealthDescription.targetHealth();
                if (health.state() != TargetHealthStateEnum.DRAINING) {
                    TargetDescription description = targetHealthDescription.target();
                    TargetResource target = new TargetResource(description);
                    target.parent(this);
                    getTarget().add(new TargetResource(description));
                }
            }

            getTags().clear();
            DescribeTagsResponse tagResponse = client.describeTags(r -> r.resourceArns(getTargetGroupArn()));
            if (tagResponse != null) {
                List<Tag> tags = tagResponse.tagDescriptions().get(0).tags();
                for (Tag tag : tags) {
                    getTags().put(tag.key(), tag.value());
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        CreateTargetGroupResponse response;

        if (getHealthCheck() != null && getHealthCheckEnabled() == true) {
            response = client.createTargetGroup(r -> r.healthCheckEnabled(getHealthCheckEnabled())
                    .healthCheckIntervalSeconds(getHealthCheck().getHealthCheckInterval())
                    .healthCheckPath(getHealthCheck().getHealthCheckPath())
                    .healthCheckPort(getHealthCheck().getHealthCheckPort())
                    .healthCheckProtocol(getHealthCheck().getHealthCheckProtocol())
                    .healthCheckTimeoutSeconds(getHealthCheck().getHealthCheckTimeout())
                    .healthyThresholdCount(getHealthCheck().getHealthyThreshold())
                    .matcher(Matcher.builder().httpCode(getHealthCheck().getMatcher()).build())
                    .port(getPort())
                    .protocol(getProtocol())
                    .name(getTargetGroupName())
                    .targetType(getTargetType())
                    .unhealthyThresholdCount(getHealthCheck().getUnhealthyThreshold())
                    .vpcId(getVpcId())
            );
        } else {
            response = client.createTargetGroup(r -> r.healthCheckEnabled(getHealthCheckEnabled())
                    .port(getPort())
                    .protocol(getProtocol())
                    .name(getTargetGroupName())
                    .targetType(getTargetType())
                    .vpcId(getVpcId()));
        }

        setTargetGroupArn(response.targetGroups().get(0).targetGroupArn());

        if (!toTargets().isEmpty()) {
            client.registerTargets(r -> r.targets(toTargets())
                    .targetGroupArn(getTargetGroupArn()));
        }

        if (!getTags().isEmpty()) {
            List<Tag> tag = new ArrayList<>();
            getTags().forEach((key, value) -> tag.add(Tag.builder().key(key).value(value).build()));
            client.addTags(r -> r.tags(tag)
                    .resourceArns(getTargetGroupArn()));
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        if (getHealthCheck() != null && getHealthCheckEnabled() == true) {
            client.modifyTargetGroup(r -> r.healthCheckEnabled(getHealthCheckEnabled())
                    .healthCheckIntervalSeconds(getHealthCheck().getHealthCheckInterval())
                    .healthCheckPath(getHealthCheck().getHealthCheckPath())
                    .healthCheckPort(getHealthCheck().getHealthCheckPort())
                    .healthCheckProtocol(getHealthCheck().getHealthCheckProtocol())
                    .healthCheckTimeoutSeconds(getHealthCheck().getHealthCheckTimeout())
                    .healthyThresholdCount(getHealthCheck().getHealthyThreshold())
                    .matcher(Matcher.builder().httpCode(getHealthCheck().getMatcher()).build())
                    .targetGroupArn(getTargetGroupArn())
                    .unhealthyThresholdCount(getHealthCheck().getUnhealthyThreshold())
            );
        }

        TargetGroupResource currentResource = (TargetGroupResource) current;

        List<TargetDescription> descriptionAdditions = new ArrayList<>(toTargets());
        descriptionAdditions.removeAll(currentResource.toTargets());

        List<TargetDescription> descriptionSubtractions = new ArrayList<>(currentResource.toTargets());
        descriptionSubtractions.removeAll(toTargets());

        if (!descriptionAdditions.isEmpty()) {
            client.registerTargets(r -> r.targets(descriptionAdditions)
                    .targetGroupArn(getTargetGroupArn()));
        }

        if (!descriptionSubtractions.isEmpty()) {
            client.deregisterTargets(r -> r.targets(descriptionSubtractions)
                    .targetGroupArn(getTargetGroupArn()));
        }

        Map<String, String> tagAdditions = new HashMap<>(getTags());
        currentResource.getTags().forEach((key, value) -> tagAdditions.remove(key, value));

        Map<String, String> tagSubtractions = new HashMap<>(currentResource.getTags());
        getTags().forEach((key, value) -> tagSubtractions.remove(key, value));

        if (!tagAdditions.isEmpty()) {
            List<Tag> tag = new ArrayList<>();
            tagAdditions.forEach((key, value) -> tag.add(Tag.builder().key(key).value(value).build()));
            client.addTags(r -> r.tags(tag)
                    .resourceArns(getTargetGroupArn()));
        }

        if (!tagSubtractions.isEmpty()) {
            List<String> tag = new ArrayList<>();
            tagSubtractions.forEach((key, value) -> tag.add(key));
            client.removeTags(r -> r.tagKeys(tag)
                    .resourceArns(getTargetGroupArn()));
        }
    }

    @Override
    public void delete() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.deleteTargetGroup(r -> r.targetGroupArn(getTargetGroupArn()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getTargetGroupName() != null) {
            sb.append("target group " + getTargetGroupName());

        } else {
            sb.append("target group ");
        }

        return sb.toString();
    }

    private List<TargetDescription> toTargets() {
        List<TargetDescription> targetDescriptions = new ArrayList<>();

        for (TargetResource target : getTarget()) {
            targetDescriptions.add(target.toTarget());
        }

        return targetDescriptions;
    }
}
