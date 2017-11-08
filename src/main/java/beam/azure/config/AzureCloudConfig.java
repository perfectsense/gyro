package beam.azure.config;

import beam.BeamException;
import beam.BeamResourceFilter;
import beam.BeamRuntime;
import beam.azure.AzureCloud;
import beam.config.CloudConfig;
import beam.config.ConfigValue;
import com.microsoft.azure.management.compute.AvailabilitySetOperations;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.AvailabilitySet;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.dns.DnsManagementClient;
import com.microsoft.azure.management.dns.ZoneOperations;
import com.microsoft.azure.management.dns.models.Zone;
import com.microsoft.azure.management.dns.models.ZoneListParameters;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.network.models.NetworkSecurityGroup;
import com.microsoft.azure.management.network.models.VirtualNetwork;
import com.microsoft.azure.management.resources.ResourceGroupOperations;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.storage.CorsProperties;
import com.microsoft.azure.storage.CorsRule;
import com.microsoft.azure.storage.ServiceProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import java.util.*;

@ConfigValue("azure")
public class AzureCloudConfig extends CloudConfig {
    private List<AzureRegionConfig> regions;
    private List<VirtualNetworkResource> vns;
    private List<ZoneResource> zones;
    private BeamResourceFilter filter;
    private List<DeploymentResource> deployments;
    private Set<BlobContainerResource> blobContainers;
    private Set<ResourceGroupResource> resourceGroups;
    private List<AzureCorsRuleResource> corsRules;

    public BeamResourceFilter getFilter() {
        return filter;
    }

    public void setFilter(BeamResourceFilter filter) {
        this.filter = filter;
    }

    public List<AzureRegionConfig> getRegions() {
        if (regions == null) {
            regions = new ArrayList<>();
        }
        return regions;
    }

    public void setRegions(List<AzureRegionConfig> regions) {
        this.regions = regions;
    }

    public List<VirtualNetworkResource> getVns() {
        if (vns == null) {
            vns = new ArrayList<>();
        }
        return vns;
    }

    public void setVns(List<VirtualNetworkResource> vns) {
        this.vns = vns;
    }

    public List<AzureCorsRuleResource> getCorsRules() {
        if (corsRules == null) {
            corsRules = new ArrayList<>();
        }
        return corsRules;
    }

    public void setCorsRules(List<AzureCorsRuleResource> corsRules) {
        this.corsRules = corsRules;
    }

    public List<ZoneResource> getZones() {
        if (zones == null) {
            zones = new ArrayList<>();
        }

        return zones;
    }

    public void setZones(List<ZoneResource> zones) {
        this.zones = zones;
    }

    public List<DeploymentResource> getDeployments() {
        if (deployments == null) {
            deployments = new ArrayList<>();
        }

        return deployments;
    }

    public void setDeployments(List<DeploymentResource> deployments) {
        this.deployments = deployments;
    }

    public Set<BlobContainerResource> getBlobContainers() {
        if (blobContainers == null) {
            blobContainers = new HashSet<>();
        }
        return blobContainers;
    }

    public void setBlobContainers(Set<BlobContainerResource> blobContainers) {
        this.blobContainers = blobContainers;
    }

    public Set<ResourceGroupResource> getResourceGroups() {
        if (resourceGroups == null) {
            resourceGroups = new HashSet<>();
        }
        return resourceGroups;
    }

    public void setResourceGroups(Set<ResourceGroupResource> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public void init(AzureCloud cloud) {
        ResourceManagementClient rMclient = cloud.createResourceManagementClient();
        ResourceGroupOperations rGOperations = rMclient.getResourceGroupsOperations();

        NetworkResourceProviderClient nRPClient = cloud.createNetworkManagementClient();
        VirtualNetworkOperations vNOperations = nRPClient.getVirtualNetworksOperations();

        DnsManagementClient dNSClient = cloud.createDnsManagementClient();
        ZoneOperations zoneOperations = dNSClient.getZonesOperations();

        BlobContainerResource containerResource = new BlobContainerResource();
        CloudBlobClient cBclient = containerResource.createClient(cloud);

        try {
            ServiceProperties blobServiceProperties = cBclient.downloadServiceProperties();
            CorsProperties cors = blobServiceProperties.getCors();

            for (CorsRule corsRule : cors.getCorsRules()) {
                AzureCorsRuleResource corsRuleResource = new AzureCorsRuleResource();
                corsRuleResource.init(cloud, filter, corsRule);
                getCorsRules().add(corsRuleResource);
            }
        } catch (Exception error){
            error.printStackTrace();
            throw new BeamException(String.format("Unable to load azure cors rule: " + error.getMessage()));
        }

        for (CloudBlobContainer container : cBclient.listContainers()) {
            containerResource = new BlobContainerResource();
            containerResource.init(cloud, filter, container);
            getBlobContainers().add(containerResource);
        }

        Set<String> resourceGroupRegions = new HashSet<>();
        resourceGroupRegions.addAll(getActiveRegions());
        resourceGroupRegions.add("eastus");

        for (String region : resourceGroupRegions) {
            String resourceGroupName = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), region);

            try {
                if (rGOperations.checkExistence(resourceGroupName).isExists()) {
                    ResourceGroupExtended resourceGroup = rGOperations.get(resourceGroupName).getResourceGroup();
                    ResourceGroupResource resourceGroupResource = new ResourceGroupResource();
                    resourceGroupResource.setRegion(region);
                    resourceGroupResource.init(cloud, getFilter(), resourceGroup);
                    getResourceGroups().add(resourceGroupResource);
                }

            } catch (Exception error) {
                error.printStackTrace();
                throw new BeamException("Fail to load Azure resource group: " + resourceGroupName);
            }
        }

        for (String region : getActiveRegions()) {
            String resourceGroupName = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), region);

            try {
                if (rGOperations.checkExistence(resourceGroupName).isExists()) {
                    for (VirtualNetwork vn : vNOperations.list(resourceGroupName).getVirtualNetworks()) {
                        VirtualNetworkResource vnResource = new VirtualNetworkResource();
                        vnResource.setRegion(region);
                        vnResource.init(cloud, getFilter(), vn);
                        getVns().add(vnResource);
                    }

                    for (Zone zone : zoneOperations.list(resourceGroupName, new ZoneListParameters()).getZones()) {
                        ZoneResource zoneResource = new ZoneResource();
                        zoneResource.setRegion("global");
                        zoneResource.init(cloud, getFilter(), zone);
                        getZones().add(zoneResource);
                    }
                }

            } catch (Exception error) {
                error.printStackTrace();
                throw new BeamException("Fail to load Azure cloud resources! ");
            }
        }

        // Deployment resource
        Map<String, DeploymentResource> deploymentResources = new HashMap<>();

        for (VirtualNetworkResource virtualNetworkResource : getVns()) {
            for (AzureGroupResource azureGroupResource : virtualNetworkResource.getAzureGroupResources()) {
                if (azureGroupResource.getTags().get("beam.verifying") != null) {
                    DeploymentResource deploymentResource = deploymentResources.get(azureGroupResource.getGroupHash());
                    if (deploymentResource == null) {
                        deploymentResource = new DeploymentResource();
                        deploymentResource.setGroupHash(azureGroupResource.getGroupHash());

                        Map<String, String> metaData = azureGroupResource.getMetaData(cloud);

                        if (!metaData.containsKey("buildNumber")) {
                            continue;
                        }

                        deploymentResource.setImage(metaData.get("image"));
                        deploymentResource.setInstanceType(metaData.get("instanceType"));

                        metaData.remove("image");
                        metaData.remove("instanceType");

                        List<String> metaList = new ArrayList<>();
                        for (String key : metaData.keySet()) {
                            String value = metaData.get(key);
                            metaList.add(key + ": " + value);
                        }

                        String deploymentString = String.join(", ", metaList);
                        deploymentResource.setDeploymentString(deploymentString);
                        deploymentResources.put(azureGroupResource.getGroupHash(), deploymentResource);
                    }

                    deploymentResource.getAutoscaleGroups().add(azureGroupResource);
                }
            }
        }

        getDeployments().addAll(deploymentResources.values());
    }
}
