package beam.aws.config;

import java.util.ArrayList;
import java.util.List;

import beam.config.Config;

public class AWSLoadBalancerConfig extends Config {

    private String name;
    private String subnetType;
    private AWSLoadBalancerDnsConfig dns;
    private List<AWSLoadBalancerListenerConfig> listeners;
    private AWSLoadBalancerHealthCheckConfig healthCheck;
    private List<String> securityRules;
    private Integer idleTimeout;
    private String scheme;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubnetType() {
        return subnetType;
    }

    public void setSubnetType(String subnetType) {
        this.subnetType = subnetType;
    }

    public AWSLoadBalancerDnsConfig getDns() {
        return dns;
    }

    public void setDns(AWSLoadBalancerDnsConfig dns) {
        this.dns = dns;
    }

    public List<AWSLoadBalancerListenerConfig> getListeners() {
        return listeners;
    }

    public void setListeners(List<AWSLoadBalancerListenerConfig> listeners) {
        this.listeners = listeners;
    }

    public AWSLoadBalancerHealthCheckConfig getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(AWSLoadBalancerHealthCheckConfig healthCheck) {
        this.healthCheck = healthCheck;
    }

    public List<String> getSecurityRules() {
        if (securityRules == null) {
            securityRules = new ArrayList<>();
        }

        return securityRules;
    }

    public void setSecurityRules(List<String> securityRules) {
        this.securityRules = securityRules;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}