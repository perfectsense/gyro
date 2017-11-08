package beam.config;

import beam.aws.AWSCloud;
import beam.aws.config.AWSCloudConfig;
import beam.aws.config.AWSRegionConfig;
import beam.aws.config.AWSZoneConfig;
import beam.aws.config.SubnetResource;
import beam.azure.config.AzureCloudConfig;
import beam.azure.config.AzureRegionConfig;
import beam.openstack.OpenStackCloud;
import beam.openstack.config.NetworkResource;
import beam.openstack.config.OpenStackCloudConfig;
import beam.openstack.config.RegionResource;
import beam.openstack.config.ServerResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkConfig extends Config {

    private String name;
    private String account;
    private String subdomain;
    private String internalDomain;
    private String serial;
    private boolean sandbox;
    private List<SecurityRuleConfig> rules;
    private List<CloudConfig> clouds;
    private List<String> plugins;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getInternalDomain() {
        return internalDomain;
    }

    public void setInternalDomain(String internalDomain) {
        this.internalDomain = internalDomain;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public List<SecurityRuleConfig> getRules() {
        if (rules == null) {
            rules = new ArrayList<>();
        }
        return rules;
    }

    public void setRules(List<SecurityRuleConfig> rules) {
        this.rules = rules;
    }

    public List<CloudConfig> getClouds() {
        if (clouds == null) {
            clouds = new ArrayList<>();
        }
        return clouds;
    }

    public void setClouds(List<CloudConfig> clouds) {
        this.clouds = clouds;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public List<ProvisionerConfig> getGatewayProvisioners(String location) {
        for (CloudConfig cloudConfig : getClouds()) {
            if (cloudConfig instanceof AWSCloudConfig) {
                AWSCloudConfig awsCloudConfig = (AWSCloudConfig) cloudConfig;

                for (AWSRegionConfig regionConfig : awsCloudConfig.getRegions()) {
                    for (AWSZoneConfig zoneConfig : regionConfig.getZones()) {
                        if (!zoneConfig.getName().equals(location)) {

                        }

                        for (SubnetConfig subnetConfig : zoneConfig.getSubnets()) {
                            GatewayConfig gatewayConfig = subnetConfig.getGateway();

                            if (gatewayConfig != null) {
                                return gatewayConfig.getProvisioners();
                            }
                        }
                    }
                }
            } else if (cloudConfig instanceof OpenStackCloudConfig) {
                OpenStackCloudConfig osCloudConfig = (OpenStackCloudConfig) cloudConfig;

                for (RegionResource region : osCloudConfig.getRegions()) {
                    if (!region.getName().equals(location)) {
                        continue;
                    }

                    for (NetworkResource networkResource : region.getSubnets()) {
                        if (networkResource.getGateway() != null) {
                            return networkResource.getGateway().getProvisioners();
                        }
                    }
                }
            } else if (cloudConfig instanceof AzureCloudConfig) {
                AzureCloudConfig azureCloudConfig = (AzureCloudConfig) cloudConfig;

                for (AzureRegionConfig region : azureCloudConfig.getRegions()) {
                    if (!region.getName().equals(location)) {
                        continue;
                    }

                    for (SubnetConfig subnetConfig : region.getSubnets()) {
                        if (subnetConfig.getGateway() != null) {
                            return subnetConfig.getGateway().getProvisioners();
                        }
                    }
                }
            }
        }

        return Collections.emptyList();
    }

}
