package beam.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlacementConfig extends Config {

    private String subnetType;
    private int sizePerSubnet;
    private AutoScaleConfig autoscale;
    private boolean isPublicAccessible;
    private Set<String> hostnames;
    private List<String> privateHostnames;
    private Boolean elasticIp = Boolean.FALSE;
    private Set<String> elasticIps;

    public String getSubnetType() {
        return subnetType;
    }

    public void setSubnetType(String subnetType) {
        this.subnetType = subnetType;
    }

    public int getSizePerSubnet() {
        return sizePerSubnet;
    }

    public void setSizePerSubnet(int sizePerSubnet) {
        this.sizePerSubnet = sizePerSubnet;
    }

    public AutoScaleConfig getAutoscale() {
        return autoscale;
    }

    public void setAutoscale(AutoScaleConfig autoscale) {
        this.autoscale = autoscale;
    }

    public boolean isPublicAccessible() {
        return isPublicAccessible;
    }

    public void setPublicAccessible(boolean isPublicAccessible) {
        this.isPublicAccessible = isPublicAccessible;
    }

    public Set<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new HashSet<>();
        }

        return hostnames;
    }

    public void setHostnames(Set<String> hostnames) {
        this.hostnames = hostnames;
    }

    public List<String> getPrivateHostnames() {
        if (privateHostnames == null) {
            privateHostnames = new ArrayList<>();
        }

        return privateHostnames;
    }

    public void setPrivateHostnames(List<String> privateHostnames) {
        this.privateHostnames = privateHostnames;
    }

    public Boolean getElasticIp() {
        return elasticIp;
    }

    public void setElasticIp(Boolean elasticIp) {
        this.elasticIp = elasticIp;
    }

    public Set<String> getElasticIps() {
        if (elasticIps == null) {
            elasticIps = new HashSet<>();
        }

        return elasticIps;
    }

    public void setElasticIps(Set<String> elasticIps) {
        this.elasticIps = elasticIps;
    }

}