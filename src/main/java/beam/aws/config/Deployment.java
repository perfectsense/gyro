package beam.aws.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Deployment {

    private Status status = Status.UNDEPLOYED;

    private String hash;

    private List<LoadBalancerResource> liveLoadBalancers;

    private List<LoadBalancerResource> verificationLoadBalancers;

    private AutoScalingGroupResource autoscaleResource;

    private AutoScalingGroupResource liveAutoscaleResource;

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

    public AutoScalingGroupResource getAutoscaleResource() {
        return autoscaleResource;
    }

    public void setAutoscaleResource(AutoScalingGroupResource autoscaleResource) {
        this.autoscaleResource = autoscaleResource;
    }

    public AutoScalingGroupResource getLiveAutoscaleResource() {
        return liveAutoscaleResource;
    }

    public void setLiveAutoscaleResource(AutoScalingGroupResource liveAutoscaleResource) {
        this.liveAutoscaleResource = liveAutoscaleResource;
    }

    public LoadBalancerResource matchingLiveLoadBalancer(LoadBalancerResource verificationLoadBalancer) {
        for (LoadBalancerResource lb : getLiveLoadBalancers()) {
            if (verificationLoadBalancer.getLoadBalancerName().startsWith(lb.getLoadBalancerName())) {
                return lb;
            }
        }

        return null;
    }

    public LoadBalancerResource matchingVerificationLoadBalanacer(LoadBalancerResource liveLoadBalancer) {
        for (LoadBalancerResource lb : getVerificationLoadBalancers()) {
            if (lb.getLoadBalancerName().startsWith(liveLoadBalancer.getLoadBalancerName())) {
                return lb;
            }
        }

        return null;
    }

    public List<String> verificationLoadBalancerNames() {
        Set<String> names = new HashSet<>();

        for (LoadBalancerResource lb : getVerificationLoadBalancers()) {
            names.add(lb.getLoadBalancerName());
        }

        return new ArrayList(names);
    }

    public List<String> liveLoadBalancerNames() {
        Set<String> names = new HashSet<>();

        for (LoadBalancerResource lb : getLiveLoadBalancers()) {
            names.add(lb.getLoadBalancerName());
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
