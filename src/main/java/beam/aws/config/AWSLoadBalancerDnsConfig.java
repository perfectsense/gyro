package beam.aws.config;

import beam.config.Config;
import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AWSLoadBalancerDnsConfig extends Config {

    private String routingPolicy;
    private Long weight;
    private Set<String> hostnames;
    private Set<String> verificationHostnames;

    public Set<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new HashSet<>();
        }

        return hostnames;
    }

    public void setHostnames(Set<String> hostnames) {
        this.hostnames = hostnames;
    }

    public Set<String> getVerificationHostnames() {
        if (verificationHostnames == null) {
            verificationHostnames = new HashSet<>();
        }

        return verificationHostnames;
    }

    public void setVerificationHostnames(Set<String> verificationHostnames) {
        this.verificationHostnames = verificationHostnames;
    }

    public String getRoutingPolicy() {
        if (ObjectUtils.isBlank(routingPolicy)) {
            routingPolicy = "simple";
        }

        return routingPolicy;
    }

    public void setRoutingPolicy(String routingPolicy) {
        List<String> values = split(routingPolicy);

        if (values.size() > 0) {
            this.routingPolicy = values.get(0).toLowerCase();
        }

        if (values.size() > 1 && "weighted".equals(this.routingPolicy)) {
            this.weight = Long.valueOf(values.get(1));
        }
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    private List<String> split(String argument) {
        List<String> split = new ArrayList<>();

        if (argument!= null) {
            split.addAll(Arrays.asList(argument.trim().split("\\s*,\\s*")));
        }

        return split;
    }
}