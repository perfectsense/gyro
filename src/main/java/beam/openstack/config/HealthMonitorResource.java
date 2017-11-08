package beam.openstack.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.HealthMonitor;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.HealthMonitorApi;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class HealthMonitorResource extends OpenStackResource<HealthMonitor> {

    private String type;
    private int delay;
    private int timeout;
    private int attempts;
    private String statusRegex;
    private String bodyRegex;
    private String path;
    private BeamReference loadBalancer;

    public BeamReference getLoadBalancer() {
        return newParentReference(LoadBalancerResource.class, loadBalancer);
    }

    public void setLoadBalancer(BeamReference loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ResourceDiffProperty(updatable = true)
    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @ResourceDiffProperty(updatable = true)
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @ResourceDiffProperty(updatable = true)
    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    @ResourceDiffProperty(updatable = true)
    public String getStatusRegex() {
        return statusRegex;
    }

    public void setStatusRegex(String statusRegex) {
        this.statusRegex = statusRegex;
    }

    @ResourceDiffProperty(updatable = true)
    public String getBodyRegex() {
        return bodyRegex;
    }

    public void setBodyRegex(String bodyRegex) {
        this.bodyRegex = bodyRegex;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public List<?> diffIds() {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource) getLoadBalancer().resolve();
        return Arrays.asList(loadBalancerResource.getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, HealthMonitor healthMonitor) {
        setAttempts(healthMonitor.getAttemptsBeforeDeactivation());
        setDelay(healthMonitor.getDelay());
        setTimeout(healthMonitor.getTimeout());
        setType(healthMonitor.getType().name());

        if (healthMonitor.getPath().isPresent()) {
            setPath(healthMonitor.getPath().get());
        }

        if (healthMonitor.getBodyRegex().isPresent()) {
            setBodyRegex(healthMonitor.getBodyRegex().get());
        }

        if (healthMonitor.getStatusRegex().isPresent()) {
            setStatusRegex(healthMonitor.getStatusRegex().get());
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource) getLoadBalancer().resolve();
        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();

        HealthMonitorApi heathApi = api.getHealthMonitorApi(loadBalancerResource.getRegion(), loadBalancerResource.getLoadBalancerId());

        HealthMonitor healthMonitor = HealthMonitor.builder()
                .bodyRegex(getBodyRegex())
                .statusRegex(getStatusRegex())
                .path(getPath())
                .timeout(getTimeout())
                .delay(getDelay())
                .attemptsBeforeDeactivation(getAttempts())
                .type(HealthMonitor.Type.fromValue(getType())).build();

        heathApi.createOrUpdate(healthMonitor);
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, HealthMonitor> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource) getLoadBalancer().resolve();
        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();

        HealthMonitorApi heathApi = api.getHealthMonitorApi(loadBalancerResource.getRegion(), loadBalancerResource.getLoadBalancerId());

        heathApi.delete();
    }

    @Override
    public String toString() {
        return String.format("[path: %s, statusRegex: %s, bodyRegex: %s, timeout: %d, delay: %d, attempts: %d]",
                getPath(), getStatusRegex(), getBodyRegex(), getTimeout(), getDelay(), getAttempts());
    }

    @Override
    public String toDisplayString() {
        return "health monitor";
    }

}