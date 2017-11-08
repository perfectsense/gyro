package beam.azure.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Deployment {

    private Status status = Status.UNDEPLOYED;

    private String hash;

    private List<LoadBalancerResource> liveLoadBalancers;

    private List<LoadBalancerResource> verificationLoadBalancers;

    private AzureGroupResource autoscaleResource;

    private AzureGroupResource liveAutoscaleResource;

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

    public AzureGroupResource getAutoscaleResource() {
        return autoscaleResource;
    }

    public void setAutoscaleResource(AzureGroupResource autoscaleResource) {
        this.autoscaleResource = autoscaleResource;
    }

    public AzureGroupResource getLiveAutoscaleResource() {
        return liveAutoscaleResource;
    }

    public void setLiveAutoscaleResource(AzureGroupResource liveAutoscaleResource) {
        this.liveAutoscaleResource = liveAutoscaleResource;
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
