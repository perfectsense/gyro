package beam.aws.config;

import java.util.ArrayList;
import java.util.List;

import beam.config.Config;
import beam.config.SubnetConfig;

public class AWSZoneConfig extends Config {

    private String name;
    private List<AWSSubnetConfig> subnets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AWSSubnetConfig> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<AWSSubnetConfig> subnets) {
        this.subnets = subnets;
    }
}
