package beam.openstack.config;

import org.jclouds.openstack.nova.v2_0.domain.Server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Deployment {

    private Status status = Status.UNDEPLOYED;

    private String hash;

    private List<LoadBalancerResource> liveLoadBalancers;

    private List<LoadBalancerResource> verificationLoadBalancers;

    private AutoscaleResource autoscaleResource;

    private AutoscaleResource liveAutoscaleResource;

    private List<Server> verificationServers;

    private List<Server> liveServers;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<LoadBalancerResource> getVerificationLoadBalancers() {
        if (verificationLoadBalancers == null) {
            verificationLoadBalancers = new ArrayList<>();
        }

        return verificationLoadBalancers;
    }

    public void setVerificationLoadBalancers(List<LoadBalancerResource> verificationLoadBalancers) {
        this.verificationLoadBalancers = verificationLoadBalancers;
    }

    public List<LoadBalancerResource> getLiveLoadBalancers() {
        if (liveLoadBalancers == null) {
            liveLoadBalancers = new ArrayList<>();
        }

        return liveLoadBalancers;
    }

    public void setLiveLoadBalancers(List<LoadBalancerResource> liveLoadBalancers) {
        this.liveLoadBalancers = liveLoadBalancers;
    }

    public AutoscaleResource getAutoscaleResource() {
        return autoscaleResource;
    }

    public void setAutoscaleResource(AutoscaleResource autoscaleResource) {
        this.autoscaleResource = autoscaleResource;
    }

    public AutoscaleResource getLiveAutoscaleResource() {
        return liveAutoscaleResource;
    }

    public void setLiveAutoscaleResource(AutoscaleResource liveAutoscaleResource) {
        this.liveAutoscaleResource = liveAutoscaleResource;
    }

    public List<Server> getVerificationServers() {
        return verificationServers;
    }

    public void setVerificationServers(List<Server> verificationServers) {
        this.verificationServers = verificationServers;
    }

    public List<Server> getLiveServers() {
        return liveServers;
    }

    public void setLiveServers(List<Server> liveServers) {
        this.liveServers = liveServers;
    }

    public LoadBalancerResource matchingLiveLoadBalancer(LoadBalancerResource verificationLoadBalancer) {
        for (LoadBalancerResource lb : getLiveLoadBalancers()) {
            if (verificationLoadBalancer.getName().startsWith(lb.getName())) {
                return lb;
            }
        }

        return null;
    }

    public LoadBalancerResource matchingVerificationLoadBalanacer(LoadBalancerResource liveLoadBalancer) {
        for (LoadBalancerResource lb : getVerificationLoadBalancers()) {
            if (lb.getName().startsWith(liveLoadBalancer.getName())) {
                return lb;
            }
        }

        return null;
    }

    public List<String> verificationLoadBalancerNames() {
        Set<String> names = new HashSet<>();

        for (LoadBalancerResource lb : getVerificationLoadBalancers()) {
            names.add(lb.getName());
        }

        return new ArrayList(names);
    }

    public List<String> liveLoadBalancerNames() {
        Set<String> names = new HashSet<>();

        for (LoadBalancerResource lb : getLiveLoadBalancers()) {
            names.add(lb.getName());
        }

        return new ArrayList(names);
    }

    public enum Status {
        UNDEPLOYED,
        LOADBALANCER_CREATED,
        INSTANCES_CREATED,
        PUSHED,
        LIVE
    }

}
