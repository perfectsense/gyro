package beam.aws.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.config.Config;

public class AWSRegionConfig extends Config {

    private String name;
    private String cidr;
    private boolean recoveryRegion;
    private List<AWSLoadBalancerConfig> loadBalancers;
    private List<AWSZoneConfig> zones;
    private List<VpcEndpointResource> endpoints;
    private Set<BucketResource> buckets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isRecoveryRegion() {
        return recoveryRegion;
    }

    public void setRecoveryRegion(boolean recoveryRegion) {
        this.recoveryRegion = recoveryRegion;
    }

    public List<AWSLoadBalancerConfig> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new ArrayList<>();
        }

        return loadBalancers;
    }

    public void setLoadBalancers(List<AWSLoadBalancerConfig> loadBalancer) {
        this.loadBalancers = loadBalancer;
    }

    public List<AWSZoneConfig> getZones() {
        if (zones == null) {
            zones = new ArrayList<>();
        }

        return zones;
    }

    public void setZones(List<AWSZoneConfig> zones) {
        this.zones = zones;
    }

    public List<VpcEndpointResource> getEndpoints() {
        if (endpoints == null) {
            endpoints = new ArrayList<>();
        }

        return endpoints;
    }

    public void setEndpoints(List<VpcEndpointResource> endpoints) {
        this.endpoints = endpoints;
    }

    public Set<BucketResource> getBuckets() {
        if (buckets == null) {
            buckets = new HashSet<>();
        }
        return buckets;
    }

    public void setBuckets(Set<BucketResource> buckets) {
        this.buckets = buckets;
    }

}
