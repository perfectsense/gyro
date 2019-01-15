package beam.aws.elb;

import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancing.model.HealthCheck;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;

import java.util.Set;

@ResourceName(parent = "classic-load-balancer", value = "health-check")
public class HealthCheckResource extends ClassicLoadBalancerResource {

    private Integer healthyThreshold;
    private Integer interval;
    private String target;
    private Integer timeout;
    private Integer unhealthyThreshold;

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    public HealthCheck toHealthCheck() {
        HealthCheck healthCheck = HealthCheck.builder()
                .healthyThreshold(getHealthyThreshold())
                .interval(getInterval())
                .target(getTarget())
                .timeout(getTimeout())
                .unhealthyThreshold(getUnhealthyThreshold())
                .build();

        return healthCheck;
    }

    @Override
    public boolean refresh() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .build();

        DescribeLoadBalancersResponse response = client.describeLoadBalancers(r -> r.loadBalancerNames(getLoadBalancerName()));

        if (response != null) {
            for (LoadBalancerDescription description : response.loadBalancerDescriptions()) {
                HealthCheck healthCheck = description.healthCheck();
                setHealthyThreshold(healthCheck.healthyThreshold());
                setInterval(healthCheck.interval());
                setTarget(healthCheck.target());
                setTimeout(healthCheck.timeout());
                setUnhealthyThreshold(healthCheck.unhealthyThreshold());
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .build();

        client.configureHealthCheck(r ->
                r.loadBalancerName(getLoadBalancerName())
                        .healthCheck(toHealthCheck()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .build();

        client.configureHealthCheck(r ->
                r.loadBalancerName(getLoadBalancerName())
                .healthCheck(toHealthCheck()));
    }

    @Override
    public void delete() {}

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("load balancer health check");
        return sb.toString();
    }
}
