package beam.config;

import java.util.Set;

public class SubnetConfig extends Config {

    private Set<String> types;
    private String cidr;
    private boolean publicAccessible = false;
    private GatewayConfig gateway;
    private boolean natGateway;
    private String natGatewayIp;

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isPublicAccessible() {
        return publicAccessible;
    }

    public void setPublicAccessible(boolean publicAccessible) {
        this.publicAccessible = publicAccessible;
    }

    public GatewayConfig getGateway() {
        return gateway;
    }

    public void setGateway(GatewayConfig gateway) {
        this.gateway = gateway;
    }

    public boolean isNatGateway() {
        return natGateway;
    }

    public void setNatGateway(boolean natGateway) {
        this.natGateway = natGateway;
    }

    public String getNatGatewayIp() {
        return natGatewayIp;
    }

    public void setNatGatewayIp(String natGatewayIp) {
        this.natGatewayIp = natGatewayIp;
    }
}
