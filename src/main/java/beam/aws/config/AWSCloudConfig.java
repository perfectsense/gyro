package beam.aws.config;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.BeamRuntime;
import beam.aws.AWSCloud;
import beam.config.CloudConfig;
import beam.config.ConfigValue;

import beam.config.LayerConfig;
import beam.config.RootConfig;
import com.amazonaws.AmazonClientException;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesByNameRequest;
import com.amazonaws.services.route53.model.VPC;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.psddev.dari.util.ObjectUtils;

@ConfigValue("ec2")
public class AWSCloudConfig extends CloudConfig {

    private BeamResourceFilter filter;
    private Set<BucketResource> buckets;
    private List<VpcResource> vpcs;
    private List<HostedZoneResource> hostedZones;
    private List<AWSRegionConfig> regions;
    private List<DeploymentResource> deployments;

    public BeamResourceFilter getFilter() {
        return filter;
    }

    public void setFilter(BeamResourceFilter filter) {
        this.filter = filter;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<BucketResource> getBuckets() {
        if (buckets == null) {
            buckets = new HashSet<>();
        }
        return buckets;
    }

    public void setBuckets(Set<BucketResource> buckets) {
        this.buckets = buckets;
    }

    public List<VpcResource> getVpcs() {
        if (vpcs == null) {
            vpcs = new ArrayList<>();
        }
        return vpcs;
    }

    public VpcResource getVpcById(String id) {
        for (VpcResource vpc : getVpcs()) {
            if (vpc.getVpcId().equals(id)) {
                return vpc;
            }
        }

        return null;
    }

    public void setVpcs(List<VpcResource> vpcs) {
        this.vpcs = vpcs;
    }

    public List<HostedZoneResource> getHostedZones() {
        if (hostedZones == null) {
            hostedZones = new ArrayList<>();
        }

        return hostedZones;
    }

    public void setHostedZones(List<HostedZoneResource> hostedZones) {
        this.hostedZones = hostedZones;
    }

    public List<AWSRegionConfig> getRegions() {
        if (regions == null) {
            regions = new ArrayList<>();
        }
        return regions;
    }

    public void setRegions(List<AWSRegionConfig> regions) {
        this.regions = regions;
    }

    public void setCheckRegions(Set<String> checkRegions) {
         setActiveRegions(checkRegions);
    }

    @Override
    public Set<String> getActiveRegions() {
        Set<String> activeRegions = super.getActiveRegions();

        if (!ObjectUtils.isBlank(activeRegions)) {
            return activeRegions;
        }

        for (AWSRegionConfig region : getRegions()) {
            if (!region.isRecoveryRegion()) {
                super.getActiveRegions().add(region.getName());
            }
        }

        return super.getActiveRegions();
    }

    public Set<String> getRecoveryRegions() {
        Set<String> recoveryRegions = new HashSet<>();

        for (AWSRegionConfig regionConfig : getRegions()) {
            if (regionConfig.isRecoveryRegion()) {
                recoveryRegions.add(regionConfig.getName());
            }
        }

        return recoveryRegions;
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

    public void init(AWSCloud cloud, String s3StandardEndPoint, List<Region> compareRegions, Set<String> buckets,
                     Set<String> hostedZoneNames) {
        java.util.logging.Logger.getLogger("com.amazonaws.http.AmazonHttpClient").setLevel(Level.WARNING);

        // Hosted zones.
        AmazonRoute53Client r53Client = new AmazonRoute53Client(cloud.getProvider());

        for (String hostedZoneName : hostedZoneNames) {
            ListHostedZonesByNameRequest zonesByNameRequest = new ListHostedZonesByNameRequest();
            zonesByNameRequest.withDNSName(hostedZoneName);
            zonesByNameRequest.withMaxItems("2");

            for (HostedZone hz : r53Client.listHostedZonesByName(zonesByNameRequest).getHostedZones()) {
                if (filter == null || filter.isInclude(hz)) {
                    HostedZoneResource hzResource = new HostedZoneResource();

                    hzResource.init(cloud, filter, hz);
                    getHostedZones().add(hzResource);
                }
            }
        }

        Map<String, DeploymentResource> deploymentResources = new HashMap<>();

        for (Region region : compareRegions) {

            com.amazonaws.regions.Region awsRegion = RegionUtils.getRegion(region.getRegionName());

            try {
                AmazonS3Client s3Client = new AmazonS3Client(cloud.getProvider());
                if (s3StandardEndPoint != null &&
                        (awsRegion.getName().equals("us-east-1") || awsRegion.getName().equals("us-west-2"))) {
                    s3Client.setEndpoint(s3StandardEndPoint);
                } else {
                    s3Client.setRegion(awsRegion);
                }

                for (String bucketName : buckets) {
                    if (s3Client.doesBucketExist(bucketName)) {
                        Bucket bucket = new Bucket();
                        bucket.setName(bucketName);

                        BucketResource resource = new BucketResource();

                        resource.init(cloud, filter, bucket);
                        getBuckets().add(resource);
                    }
                }

                AmazonEC2Client ec2Client = new AmazonEC2Client(cloud.getProvider());
                ec2Client.setRegion(awsRegion);

                BeamResourceFilter filter = getFilter();

                List<CompletableFuture> vpcFutures = new ArrayList<>();
                for (Vpc vpc : ec2Client.describeVpcs().getVpcs()) {
                    if (BeamResource.isInclude(filter, vpc)) {
                        VpcResource vpcResource = new VpcResource();
                        vpcResource.setRegion(awsRegion);

                        vpcResource.initAsync(vpcFutures, cloud, filter, vpc);
                        getVpcs().add(vpcResource);
                    }
                }

                BeamResource.pollFutures(vpcFutures);
            } catch (AmazonClientException ace) {
                if (ace.getCause() instanceof ConnectException) {
                    throw new BeamException("Unable to connect to " + awsRegion.getName() + " region's endpoint. Delete " + awsRegion.getName() + " in 'checkRegions' to skip region.\n");
                }

                throw ace;
            }
        }

        // Associated vpc resources to private hosted zones
        for (HostedZoneResource hzResource : getHostedZones()) {
            if (hzResource.isPrivate()) {
                for (VPC r53Vpc : hzResource.getVpcs()) {
                    VpcResource vpcResource = getVpcById(r53Vpc.getVPCId());
                    if (vpcResource != null) {
                        hzResource.getVpcResources().add(hzResource.newReference(vpcResource));
                    }
                }
            }
        }

        // Deployment resource
        RootConfig config = BeamRuntime.getCurrentRuntime().getConfig();
        for (VpcResource vpcResource : getVpcs()) {
            for (AutoScalingGroupResource autoScaleResource : vpcResource.getAutoScalingGroups()) {
                for (AutoScalingGroupTagResource tag : autoScaleResource.getTags()) {
                    if (ObjectUtils.to(boolean.class, "beam.layer".equals(tag.getKey()))) {
                        LayerConfig layerConfig = config.getLayerByName(tag.getValue());
                        if (layerConfig != null) {
                            autoScaleResource.setDeployment(layerConfig.getDeployment());
                        }
                    }

                    if (ObjectUtils.to(boolean.class, "beam.verifying".equals(tag.getKey()))) {
                        DeploymentResource deploymentResource = deploymentResources.get(autoScaleResource.getGroupHash());
                        if (deploymentResource == null) {
                            deploymentResource = new DeploymentResource();
                            deploymentResource.setGroupHash(autoScaleResource.getGroupHash());

                            Map<String, String> metaData = autoScaleResource.getMetaData(cloud);

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
                            deploymentResources.put(autoScaleResource.getGroupHash(), deploymentResource);
                        }

                        deploymentResource.getAutoscaleGroups().add(autoScaleResource);
                    }
                }
            }
        }

        getDeployments().addAll(deploymentResources.values());
    }
}
