package beam.azure.config;

import java.util.ArrayList;
import java.util.List;
import beam.config.Config;
import beam.config.SubnetConfig;

public class AzureRegionConfig extends Config {

    private String name;
    private String cidr;
    private boolean recoveryRegion;
    private List<SubnetConfig> subnets;
    private List<AzureLoadBalancerConfig> loadBalancers;

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

    public List<SubnetConfig> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<SubnetConfig> subnets) {
        this.subnets = subnets;
    }

    public List<AzureLoadBalancerConfig> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new ArrayList<>();
        }

        return loadBalancers;
    }

    public void setLoadBalancers(List<AzureLoadBalancerConfig> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }
}
