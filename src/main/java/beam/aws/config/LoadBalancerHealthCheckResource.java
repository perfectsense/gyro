package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;

public class LoadBalancerHealthCheckResource extends AWSResource<HealthCheck> {

    private Integer healthyThreshold;
    private Integer interval;
    private BeamReference loadBalancer;
    private String target;
    private Integer timeout;
    private Integer unhealthyThreshold;

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

    public BeamReference getLoadBalancer() {
        return newParentReference(LoadBalancerResource.class, loadBalancer);
    }

    public void setLoadBalancer(BeamReference loadBalancer) {
        this.loadBalancer = loadBalancer;
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
        HealthCheck healthCheck = new HealthCheck();

        healthCheck.setHealthyThreshold(getHealthyThreshold());
        healthCheck.setInterval(getInterval());
        healthCheck.setTarget(getTarget());
        healthCheck.setTimeout(getTimeout());
        healthCheck.setUnhealthyThreshold(getUnhealthyThreshold());

        return healthCheck;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getLoadBalancer().awsId());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, HealthCheck healthCheck) {
        setHealthyThreshold(healthCheck.getHealthyThreshold());
        setInterval(healthCheck.getInterval());
        setTarget(healthCheck.getTarget());
        setTimeout(healthCheck.getTimeout());
        setUnhealthyThreshold(healthCheck.getUnhealthyThreshold());
    }

    @Override
    public void create(AWSCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, HealthCheck> current, Set<String> changedProperties) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        ConfigureHealthCheckRequest chcRequest = new ConfigureHealthCheckRequest();

        chcRequest.setHealthCheck(toHealthCheck());
        chcRequest.setLoadBalancerName(getLoadBalancer().awsId());
        client.configureHealthCheck(chcRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplayString() {
        return "load balancer health check";
    }
}
