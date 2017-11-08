package beam.aws.config;

import beam.config.SubnetConfig;

import java.util.List;
import java.util.ArrayList;

public class AWSSubnetConfig extends SubnetConfig {
    private List<String> endpoints;

    public List<String> getEndpoints() {
        if (endpoints == null) {
            endpoints = new ArrayList<>();
        }

        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }
}
