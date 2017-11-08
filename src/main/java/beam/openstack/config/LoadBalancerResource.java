package beam.openstack.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceChange;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.*;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.HealthMonitorApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoadBalancerResource extends OpenStackResource<LoadBalancer> {

    private Integer loadBalancerId;
    private String name;
    private String protocol;
    private int port;
    private int externalPort;
    private String virtualIp4;
    private Map<String, String> metadata;
    private List<String> hostnames;
    private List<String> verificationHostnames;
    private HealthMonitorResource healthMonitor;

    private LoadBalancer loadBalancer;

    public Integer getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(Integer loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        if (protocol == null) {
            protocol = "HTTP";
        }

        return protocol.toUpperCase();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public void setExternalPort(int externalPort) {
        this.externalPort = externalPort;
    }

    public String getVirtualIp4() {
        return virtualIp4;
    }

    public void setVirtualIp4(String virtualIp4) {
        this.virtualIp4 = virtualIp4;
    }

    public List<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new ArrayList<>();
        }

        return hostnames;
    }

    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public List<String> getVerificationHostnames() {
        if (verificationHostnames == null) {
            verificationHostnames = new ArrayList<>();
        }

        return verificationHostnames;
    }

    public void setVerificationHostnames(List<String> verificationHostnames) {
        this.verificationHostnames = verificationHostnames;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public HealthMonitorResource getHealthMonitor() {
        return healthMonitor;
    }

    public void setHealthMonitor(HealthMonitorResource healthMonitor) {
        this.healthMonitor = healthMonitor;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, LoadBalancer> current) throws Exception {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource) current;
        update.updateOne(loadBalancerResource.getHealthMonitor(), getHealthMonitor());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.deleteOne(getHealthMonitor());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, LoadBalancer loadBalancer) {
        setName(loadBalancer.getName());
        setPort(loadBalancer.getPort());
        setProtocol(loadBalancer.getProtocol());
        setRegion(loadBalancer.getRegion());
        setLoadBalancerId(loadBalancer.getId());
        setLoadBalancer(loadBalancer);

        for (VirtualIPWithId ip : loadBalancer.getVirtualIPs()) {
            if (ip instanceof VirtualIPWithId && ip.getType() == VirtualIP.Type.PUBLIC) {
                setVirtualIp4(ip.getAddress());
                break;
            }
        }

        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();
        HealthMonitorApi healthMonitorApi = api.getHealthMonitorApi(getRegion(), getLoadBalancerId());
        HealthMonitor healthMonitor = healthMonitorApi.get();
        if (healthMonitor != null) {
            HealthMonitorResource healthMonitorResource = new HealthMonitorResource();
            healthMonitorResource.setRegion(getRegion());
            healthMonitorResource.init(cloud, filter, healthMonitor);
            healthMonitorResource.setLoadBalancer(healthMonitorResource.newReference(this));

            setHealthMonitor(healthMonitorResource);
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {
        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = api.getLoadBalancerApi(getRegion());

        CreateLoadBalancer clb = new CreateLoadBalancer.Builder()
                .name(getName())
                .protocol(getProtocol())
                .port(getPort())
                .algorithm(CreateLoadBalancer.Algorithm.LEAST_CONNECTIONS)
                .virtualIPType(VirtualIP.Type.PUBLIC)
                .build();

        LoadBalancer loadBalancer = lbApi.create(clb);

        if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
            throw new BeamException("Timed out creating load balancer.");
        }

        init(cloud, null, loadBalancer);

        if (getHealthMonitor() != null) {
            getHealthMonitor().create(cloud);
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, LoadBalancer> current, Set<String> changedProperties) {
        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = api.getLoadBalancerApi(getRegion());

        UpdateLoadBalancer ulb = UpdateLoadBalancer.builder()
                .port(getPort())
                .protocol(getProtocol()).build();

        lbApi.update(getLoadBalancer().getId(), ulb);
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        CloudLoadBalancersApi clbApi = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = clbApi.getLoadBalancerApi(getRegion());

        lbApi.delete(getLoadBalancerId());
    }

    @Override
    public String toString() {
        return toDisplayString();
    }

    @Override
    public String toDisplayString() {
        return "load balancer " + getName();
    }
}