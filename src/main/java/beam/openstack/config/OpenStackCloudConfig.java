package beam.openstack.config;

import beam.BeamResourceFilter;
import beam.config.CloudConfig;
import beam.config.ConfigValue;
import beam.config.DeploymentConfig;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Sets;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.features.DomainApi;

import java.util.*;

@ConfigValue("openstack")
public class OpenStackCloudConfig extends CloudConfig {

    private String account;
    private String subdomain;
    private String subdomainEmail;
    private List<RegionResource> regions;
    private List<DomainResource> domains;
    private List<DeploymentResource> deployments;
    private BeamResourceFilter filter;
    private List<String> checkRegions;

    public void init(OpenStackCloud cloud, Set<String> activeRegions) {
        if (ObjectUtils.isBlank(activeRegions)) {
            activeRegions = Sets.newHashSet("IAD", "DFW", "ORD");
        }

        Map<String, DeploymentResource> deploymentResources = new HashMap<>();

        for (String region : activeRegions) {
            region = region.toUpperCase();

            // Region.
            RegionResource regionResource = new RegionResource();
            regionResource.setName(region);
            regionResource.init(cloud, filter, null);

            getRegions().add(regionResource);

            // Autoscale groups.
            AutoscaleApi autoscaleApi = cloud.createAutoscaleApi();
            GroupApi groupApi = autoscaleApi.getGroupApi(region);
            for (GroupState groupState : groupApi.listGroupStates()) {
                try {
                    Group group = groupApi.get(groupState.getId());

                    if (filter.isInclude(group)) {
                        AutoscaleResource autoscaleResource = new AutoscaleResource();
                        autoscaleResource.setRegion(region);
                        autoscaleResource.init(cloud, filter, group);

                        DeploymentResource deploymentResource = deploymentResources.get(autoscaleResource.getGroupHash());
                        if (deploymentResource == null) {
                            deploymentResource = new DeploymentResource();
                            deploymentResource.setGroupHash(autoscaleResource.getGroupHash());
                            deploymentResource.setRegion(region);

                            if (autoscaleResource.getMetadataItem("buildNumber") != null) {
                                deploymentResource.setImage(autoscaleResource.getLaunchConfig().getImage());
                                deploymentResource.setInstanceType(autoscaleResource.getLaunchConfig().getFlavor());

                                Map<String, String> metaData = autoscaleResource.getMetadata();
                                List<String> metaList = new ArrayList<>();

                                try {
                                    if (!metaData.containsKey("type")) {
                                        metaList.add("buildNumber: " + metaData.get("buildNumber"));
                                        metaList.add("jenkinsBucket: " + metaData.get("jenkinsBucket"));
                                        metaList.add("jenkinsBuildPath: " + metaData.get("jenkinsBuildPath"));
                                        metaList.add("jenkinsWarFile: " + metaData.get("jenkinsWarFile"));

                                    } else {
                                        String pluginName = metaData.get("type");
                                        Class<?> plugin = Class.forName(pluginName);
                                        DeploymentConfig deployment = (DeploymentConfig)plugin.getConstructor().newInstance();

                                        for (String key : deployment.getGroupHashKeys()) {
                                            String value = metaData.get(key);
                                            metaList.add(key + ": " + value);
                                        }
                                    }

                                } catch (Exception error) {
                                    error.printStackTrace();
                                }

                                String deploymentString = String.join(", ", metaList);
                                deploymentResource.setDeploymentString(deploymentString);
                            }

                            deploymentResources.put(autoscaleResource.getGroupHash(), deploymentResource);
                        }

                        deploymentResource.getAutoscaleGroups().add(autoscaleResource);
                        getDeployments().add(deploymentResource);
                    }
                } catch (NullPointerException npe) {
                    // Workaround bug in jclouds that causes a NPE when "personalities" isn't set.
                    System.out.println("Skipping autoscale group " + groupState.getId() + " due to missing data.");
                }
            }
        }

        // Cloud DNS.
        CloudDNSApi dnsApi = cloud.createCloudDnsApi();
        DomainApi domainApi = dnsApi.getDomainApi();
        for (Domain domain : domainApi.list().concat()) {
            if (filter.isInclude(domain)) {
                DomainResource domainResource = new DomainResource();
                domainResource.init(cloud, filter, domain);

                getDomains().add(domainResource);
            }
        }
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

    public String getSubdomainEmail() {
        return subdomainEmail;
    }

    public void setSubdomainEmail(String subdomainEmail) {
        this.subdomainEmail = subdomainEmail;
    }

    public List<RegionResource> getRegions() {
        if (regions == null) {
            regions = new ArrayList<>();
        }

        return regions;
    }

    public void setRegions(List<RegionResource> regions) {
        this.regions = regions;
    }

    public List<DomainResource> getDomains() {
        if (domains == null) {
            domains = new ArrayList<>();
        }

        return domains;
    }

    public void setDomains(List<DomainResource> domains) {
        this.domains = domains;
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

    public BeamResourceFilter getFilter() {
        return filter;
    }

    public void setFilter(BeamResourceFilter filter) {
        this.filter = filter;
    }
}