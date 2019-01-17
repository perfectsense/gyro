package beam.aws.elb;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.HealthCheck;

import java.util.Set;

/**
 * Creates a Health Check Resource
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     health-check
 *        healthy-threshold: "2"
 *        interval: "30"
 *        target: "HTTP:80/png"
 *        timeout: "3"
 *        unhealthy-threshold: "2"
 *     end
 */

@ResourceName(parent = "load-balancer", value = "health-check")
public class HealthCheckResource extends AwsResource {

    private Integer healthyThreshold;
    private Integer interval;
    private String target;
    private Integer timeout;
    private Integer unhealthyThreshold;

    public HealthCheckResource(){}

    @ResourceDiffProperty(updatable = true)
    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    @ResourceDiffProperty(updatable = true)
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @ResourceDiffProperty(updatable = true)
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

    public String getLoadBalancer() {
        LoadBalancerResource parent = (LoadBalancerResource) parentResource();
        if (parent != null) {
            return parent.getLoadBalancerName();
        }

        return null;
    }

    @Override
    public boolean refresh() {
        return true;
    }

    @Override
    public void create() {
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);

        client.configureHealthCheck(r ->
                r.loadBalancerName(getLoadBalancer())
                        .healthCheck(toHealthCheck()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        create();
    }

    @Override
    public void delete() {}

    @Override
    public String toDisplayString() {
        return String.format(
                "load balancer health check %d/%d/%s/%d/%d",
                getHealthyThreshold(),
                getInterval(),
                getTarget(),
                getTimeout(),
                getUnhealthyThreshold());
    }
}
