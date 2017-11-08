package beam.azure.config;

import beam.config.Config;
import java.util.ArrayList;
import java.util.List;

public class AzureLoadBalancerConfig extends Config {
    private String name;
    private String subnetType;
    private List<String> securityRules;
    private List<AzureLoadBalancerListenerConfig> listeners;
    private String tier;
    private AzureLoadBalancerProbeConfig probe;
    private int idleTimeout = 5;
    private List<String> hostnames;
    private List<String> verificationHostnames;
    private String internalSubnet;

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

    public List<AzureLoadBalancerListenerConfig> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        return listeners;
    }

    public void setListeners(List<AzureLoadBalancerListenerConfig> listeners) {
        this.listeners = listeners;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public AzureLoadBalancerProbeConfig getProbe() {
        return probe;
    }

    public void setProbe(AzureLoadBalancerProbeConfig probe) {
        this.probe = probe;
    }

    public List<String> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<String> securityRules) {
        this.securityRules = securityRules;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
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

    public String getInternalSubnet() {
        return internalSubnet;
    }

    public void setInternalSubnet(String internalSubnet) {
        this.internalSubnet = internalSubnet;
    }
}
