package beam.aws;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.awt.Desktop;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;

import beam.BeamResource;
import beam.BeamReference;
import beam.BeamException;
import beam.aws.config.*;
import beam.cli.ConsoleCommand;
import beam.config.*;
import beam.enterprise.EnterpriseApi;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.model.*;

import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.diff.ResourceDiff;
import beam.diff.ResourceUpdate;
import beam.diff.Change;
import beam.diff.Diff;
import beam.diff.ResourceChange;
import beam.diff.ChangeType;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import org.yaml.snakeyaml.Yaml;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import static beam.aws.config.HostedZoneRRSetResource.RoutingType;

public class AWSCloud extends BeamCloud {

    private static final int BUCKET_NAME_MAXIMUM = 63;
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSCloud.class);
    private static final Pattern ARN_PATTERN = Pattern.compile("arn:aws:([^:]*):([^:]*):([^:]*):(.*)");

    private final AWSCredentialsProvider provider;
    private final String project;
    private final String account;
    private final String serial;

    private String defaultRegion;
    private String s3StandardEndPoint;

    /**
     * @param runtime Can't be {@code null}.
     */
    public AWSCloud(BeamRuntime runtime) {
        Preconditions.checkNotNull(runtime, "runtime");

        String account = runtime.getAccount();
        String project = runtime.getProject();
        AWSCredentialsProvider provider = new ProfileCredentialsProvider(account);

        if (EnterpriseApi.isAvailable(project)) {
            provider = new EnterpriseCredentialsProviderChain(
                    new EnterpriseCredentialsProvider(account, project),
                    provider,
                    new InstanceProfileCredentialsProvider());
        }

        this.provider = provider;
        this.project = project;
        this.serial = runtime.getSerial();
        this.account = runtime.getAccount();
    }

    /**
     * @param project Can't be {@code null}.
     * @param serial Can't be {@code null}.
     */
    public AWSCloud(String project, String serial) {
        Preconditions.checkNotNull(project, "project");
        Preconditions.checkNotNull(serial, "serial");

        this.provider = new InstanceProfileCredentialsProvider();
        this.project = project;
        this.serial = serial;
        this.account = null;
    }

    public AWSCredentialsProvider getProvider() {
        if (provider instanceof ProfileCredentialsProvider) {
            ((ProfileCredentialsProvider) provider).getCredentials();
        }

        return provider;
    }

    public String getProject() {
        return project;
    }

    public String getSerial() {
        return serial;
    }

    public String getAccount() {
        return account;
    }

    public String findOldDefaultBucketName() {

        AWSCredentialsProvider provider = getProvider();
        AWSCredentials creds = provider.getCredentials();
        String arn = null;

        if (creds instanceof EnterpriseCredentials) {
            arn = ((EnterpriseCredentials) creds).getUserArn();

        } else if (creds instanceof AWSSessionCredentials) {

            try {
                File credsFile = EnterpriseApi.prepareLocalFile("aws-temporary-credentials-" + getAccount() + "-" + getProject());

                Map<String, Object> credsMap = (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(credsFile, Charsets.UTF_8));
                arn = ObjectUtils.to(String.class, credsMap.get("userArn"));

            } catch (Exception error) {

            }
        }

        if (arn == null) {
            AmazonIdentityManagementClient imClient = new AmazonIdentityManagementClient(provider);
            imClient.setRegion(getDefaultAWSRegion());

            try {
                arn = imClient.getUser().getUser().getArn();

            } catch (AmazonServiceException error) {
                if ("AccessDenied".equals(error.getErrorCode())) {
                    String message = error.getMessage();
                    Matcher arnMatcher = ARN_PATTERN.matcher(message);

                    if (arnMatcher.find()) {
                        arn = arnMatcher.group(0);
                    }
                }
            }
        }

        if (arn != null) {
            Matcher arnMatcher = ARN_PATTERN.matcher(arn);

            if (arnMatcher.matches()) {
                String accountId = arnMatcher.group(3);
                BeamRuntime runtime = BeamRuntime.getCurrentRuntime();
                String bucketName = String.format(
                        "%s-%s-%s-%s",
                        runtime.getProject(),
                        runtime.getEnvironment(),
                        runtime.getSerial(),
                        StringUtils.hex(StringUtils.sha1(accountId)));
                if (bucketName.length() > BUCKET_NAME_MAXIMUM) {
                    bucketName = bucketName.substring(0, BUCKET_NAME_MAXIMUM);
                }

                return bucketName;
            }
        }

        throw new IllegalStateException("Can't infer AWS account ID!");
    }

    @Override
    public String getName() {
        return "ec2";
    }

    private List<EC2Instance> getInstancesFromEnterprise(boolean cacheOk) {
        List<EC2Instance> instances = new ArrayList<EC2Instance>();

        try {
            Map<String, Object> instancesMap = EnterpriseApi.call("aws/instances",
                    new BasicNameValuePair("accountName", getAccount()),
                    new BasicNameValuePair("projectName", getProject()),
                    new BasicNameValuePair("serial", getSerial()),
                    new BasicNameValuePair("refresh", (cacheOk != true ? "true" : "false")));

            String status = ObjectUtils.to(String.class, instancesMap.get("status"));

            if (status != null) {
                throw new BeamException("Invalid request: " + status);
            }

            for (String instanceId : instancesMap.keySet()) {
                EC2Instance instance = new EC2Instance(this, (Map<String, Object>) instancesMap.get(instanceId));
                for (String activeRegion : getActiveRegions()) {
                    if (activeRegion.equals(instance.getRegion())) {
                        instances.add(instance);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new BeamException("Failed to retrieve instances from Beam Enteprise.");
        }

        Collections.sort(instances);

        return instances;
    }

    public List<EC2Instance> getInstancesByEnvironment(boolean cacheOk, String env) {
        List<EC2Instance> instances = getInstances(cacheOk);
        Iterator<EC2Instance> instanceIterator = instances.iterator();
        while (instanceIterator.hasNext()) {
            EC2Instance instance = instanceIterator.next();
            if (!instance.getEnvironment().equals(env)) {
                instanceIterator.remove();
            }
        }
        return instances;
    }

    @Override
    public List<EC2Instance> getInstances(boolean cacheOk) {
        if (EnterpriseApi.isAvailable()) {
            return getInstancesFromEnterprise(cacheOk);
        }

        List<EC2Instance> instances = new ArrayList<>();

        java.util.logging.Logger.getLogger("com.amazonaws.http.AmazonHttpClient").setLevel(Level.WARNING);

        for (String activeRegion : getActiveRegions()) {
            Region region = new Region().withRegionName(activeRegion);

            AWSCredentialsProvider provider = getProvider();
            String regionName = region.getRegionName().replace("-", "_").toUpperCase();
            String project = getProject();
            String serial = getSerial();

            LOGGER.debug(
                    "Provider: {}, Region: {}, Project: {}, Serial: {}",
                    new Object[] {
                            provider.getClass().getName(),
                            regionName,
                            project,
                            serial });

            AmazonEC2Client regionClient = new AmazonEC2Client(provider);
            regionClient.setRegion(RegionUtils.getRegion(region.getRegionName()));

            DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withFilters(
                    new Filter("tag:beam.project").withValues(project),
                    new Filter("tag:beam.serial").withValues(serial));

            try {
                for (Reservation r : regionClient.describeInstances(instancesRequest).getReservations()) {
                    for (Instance i : r.getInstances()) {
                        String state = i.getState().getName();

                        if (InstanceStateName.Running.toString().equals(state) ||
                                InstanceStateName.Stopped.toString().equals(state) ||
                                InstanceStateName.Stopping.toString().equals(state)) {
                            instances.add(new EC2Instance(this, i));
                        }
                    }
                }
            } catch (com.amazonaws.AmazonClientException ce) {
                if (ce.getCause() instanceof ConnectException) {
                    System.out.println("Unable to connect to " + regionName + " region's endpoint. Some instances may be missing.\n");
                } else {
                    throw ce;
                }
            }
        }
        Collections.sort(instances);
        return instances;
    }

    @Override
    public List<Diff<?, ?, ?>> findChanges(BeamRuntime runtime) throws Exception {
        RootConfig config = runtime.getConfig();

        if (config == null) {
            return null;
        }

        NetworkConfig networkConfig = config.getNetworkConfig();

        for (CloudConfig cloudConfig : networkConfig.getClouds()) {
            if (!(cloudConfig instanceof AWSCloudConfig)) {
                continue;
            }

            // Read the current state from AWS.
            AWSCloudConfig current = new AWSCloudConfig();
            AWSCloudConfig pending = (AWSCloudConfig) cloudConfig;

            for (AWSRegionConfig region : pending.getRegions()) {
                com.amazonaws.regions.Region awsRegion = RegionUtils.getRegion(region.getName());

                for (BucketResource bucket : region.getBuckets()) {
                    bucket.setRegion(awsRegion);
                    pending.getBuckets().add(bucket);
                }
            }

            Set<String> recoveryRegions = pending.getRecoveryRegions();

            // Remove inactive regions.
            Iterator<AWSRegionConfig> iter = pending.getRegions().iterator();
            while (iter.hasNext()) {
                AWSRegionConfig region = iter.next();
                if (!pending.getActiveRegions().contains(region.getName())) {
                    iter.remove();
                }
            }

            Set<String> compareRegionNames = new HashSet<>();

            compareRegionNames.addAll(pending.getActiveRegions());
            compareRegionNames.addAll(recoveryRegions);

            List<Region> compareRegions = new ArrayList<>();
            for (String regionName : compareRegionNames) {
                compareRegions.add(new Region().withRegionName(regionName));
            }

            // Hosted zones.
            String domain = networkConfig.getSubdomain();
            String domainDot = StringUtils.ensureEnd(domain, ".");
            String privateDomainDot = "private." + domainDot;
            HostedZoneResource hzResource = new HostedZoneResource();
            hzResource.setName(domainDot);
            pending.getHostedZones().add(hzResource);

            HostedZoneResource privateHzResource = new HostedZoneResource();
            privateHzResource.setName(privateDomainDot);

            AWSResourceFilter filter = new AWSResourceFilter();
            filter.setIncludedLayers(getIncludedLayers());

            Set<String> bucketNames = new HashSet<>();
            for (BucketResource bucketResource : pending.getBuckets()) {
                putTags(runtime, bucketResource, bucketResource.getName());

                bucketNames.add(bucketResource.getName());
            }
            bucketNames.add(findOldDefaultBucketName());

            Set<String> hostedZoneNames = new HashSet<>();
            hostedZoneNames.add(hzResource.getName());
            hostedZoneNames.add(privateHzResource.getName());

            current.setFilter(filter);
            current.init(this, getS3StandardEndPoint(), compareRegions, bucketNames, hostedZoneNames);

            // Create the PSD-specific pending config.
            pending.setFilter(filter);

            Map<String, HostedZoneRRSetResource> gatewayDnsMap = new HashMap<>();
            Map<String, HostedZoneRRSetResource> gatewayPrivateDnsMap = new HashMap<>();
            Map<String, HostedZoneRRSetResource> layerPrivateDnsMap = new HashMap<>();

            for (AWSRegionConfig region : pending.getRegions()) {
                com.amazonaws.regions.Region awsRegion = RegionUtils.getRegion(region.getName());

                VpcResource vpcResource = new VpcResource();
                vpcResource.setRegion(awsRegion);

                pending.getVpcs().add(vpcResource);
                vpcResource.setCidrBlock(region.getCidr());
                vpcResource.setEnableDnsHostnames(Boolean.TRUE);
                putTags(runtime, vpcResource, String.format("%s v%s", project, serial));

                // Private Hosted Zone
                privateHzResource.getVpcResources().add(privateHzResource.newReference(vpcResource));

                // Vpc endpoints
                Map<String, VpcEndpointResource> endpointResourceByName = new HashMap<>();

                for (VpcEndpointResource vpcEndpointResource : region.getEndpoints()) {

                    vpcEndpointResource.setRegion(awsRegion);
                    vpcEndpointResource.prepareService(region.getName());
                    vpcEndpointResource.preparePolicy();

                    vpcResource.getVpcEndpoints().add(vpcEndpointResource);
                    endpointResourceByName.put(vpcEndpointResource.getName(), vpcEndpointResource);
                }

                // Pre-fetch the AMI infos.
                Map<String, Image> images = new HashMap<>();
                Set<String> imageIds = new HashSet<>();
                Set<String> imageNames = new HashSet<>();

                for (AWSZoneConfig zone : region.getZones()) {
                    for (SubnetConfig subnet : zone.getSubnets()) {
                        GatewayConfig gateway = subnet.getGateway();

                        if (gateway != null) {
                            addImageIdOrName(imageIds, imageNames, gateway.getImage());
                        }
                    }
                }

                for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    addImageIdOrName(imageIds, imageNames, layer.getImage());
                }

                AmazonEC2Client ec2Client = new AmazonEC2Client(getProvider());
                ec2Client.setRegion(awsRegion);

                if (!imageIds.isEmpty()) {
                    populateImages(images, ec2Client, new DescribeImagesRequest().
                            withImageIds(imageIds));
                }

                if (!imageNames.isEmpty()) {
                    populateImages(images, ec2Client, new DescribeImagesRequest().
                            withFilters(Arrays.asList(
                                    new Filter("name").withValues(imageNames))));
                }

                // Translate region config to VPC resources.
                String project = runtime.getProject();
                String environment = runtime.getEnvironment();
                String serial = runtime.getSerial();
                String internalDomain = runtime.getInternalDomain();
                String sandbox = String.valueOf(networkConfig.isSandbox());

                // Internet gateway for the VPC.
                InternetGatewayResource igResource = new InternetGatewayResource();
                igResource.setRegion(awsRegion);

                vpcResource.setInternetGateway(igResource);
                igResource.setBeamId("ig");
                putTags(runtime, igResource, String.format("%s v%s", project, serial));

                // Key pair for the VPC.
                KeyPairResource kpResource = new KeyPairResource();
                kpResource.setRegion(awsRegion);
                String keyName = String.format("%s-%s", project, region.getName());

                vpcResource.getKeyPairs().add(kpResource);
                kpResource.setKeyName(keyName);

                // Roles and instance profiles for each layer...
                Map<String, BeamReference> ipReferenceByLayerName = new HashMap<>();

                for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    String layerName = layer.getName();
                    Set<String> policyNames = layer.getRolePolicies();

                    createRole(
                            vpcResource,
                            ipReferenceByLayerName,
                            layerName,
                            layer.getInstanceRole(),
                            policyNames);
                }

                // ...and the gateway.
                CREATE_GATEWAY_ROLE: for (AWSZoneConfig zone : region.getZones()) {
                    for (SubnetConfig subnet : zone.getSubnets()) {
                        GatewayConfig gateway = subnet.getGateway();

                        if (gateway != null) {
                            Set<String> policyNames = gateway.getRolePolicies();

                            createRole(
                                    vpcResource,
                                    ipReferenceByLayerName,
                                    "gateway",
                                    gateway.getInstanceRole(),
                                    policyNames);

                            break CREATE_GATEWAY_ROLE;
                        }
                    }
                }

                // Apply security rule configs to the VPC.
                Map<String, SecurityGroupResource> sgResourceByRuleName = new HashMap<>();
                String regionName = region.getName();

                for (SecurityRuleConfig rule : networkConfig.getRules()) {
                    SecurityGroupResource sgResource = new SecurityGroupResource();
                    String ruleName = rule.getName();
                    String sgName = String.format("%s-%s-%s-v%s", project, ruleName, regionName, serial);

                    vpcResource.getSecurityGroups().add(sgResource);
                    sgResourceByRuleName.put(ruleName, sgResource);
                    sgResource.setBeamId("sg-" + ruleName);
                    sgResource.setGroupName(sgName);
                    sgResource.setDescription(sgName);
                    sgResource.setRegion(awsRegion);
                    putTags(runtime, sgResource, sgName.replace("-", " "));
                }

                // Security group IP permissions.
                for (SecurityRuleConfig rule : networkConfig.getRules()) {
                    String ruleName = rule.getName();
                    SecurityGroupResource sgResource = sgResourceByRuleName.get(ruleName);
                    SecurityGroupIpPermissionResource permResource = new SecurityGroupIpPermissionResource();

                    if (!rule.isExcludeSelf()) {
                        // add itself so that things can talk to each
                        // other within the security group.
                        sgResource.getIpPermissions().add(permResource);
                        sgResource.setRegion(awsRegion);
                        permResource.setIpProtocol("-1");
                        permResource.setFromGroup(permResource.newReference(sgResource));
                        permResource.setRegion(awsRegion);
                    }

                    for (AccessPermissionConfig perm : rule.getPermissions()) {
                        String cidr = perm.getCidr();
                        List<Integer> ports = perm.getPorts();

                        // If no ports are specified add port "-1" to allow all traffic.
                        if (ObjectUtils.isBlank(ports)) {
                            ports.add(-1);
                        }

                        for (Integer port : ports) {
                            permResource = new SecurityGroupIpPermissionResource();
                            permResource.setRegion(awsRegion);

                            if (port == -1) {
                                permResource.setIpProtocol("-1");
                            } else {
                                permResource.setIpProtocol(perm.getProtocol());
                                permResource.setFromPort(port);
                                permResource.setToPort(port);
                            }

                            if (Character.isDigit(cidr.charAt(0))) {
                                permResource.setIpRange(cidr);
                            } else {
                                permResource.setFromGroup(permResource.newReference(sgResourceByRuleName.get(cidr)));
                            }

                            sgResource.getIpPermissions().add(permResource);
                        }
                    }
                }

                // Subnets in the VPC.
                Map<String, List<SubnetResource>> subnetResourcesByType = new HashMap<>();
                Set<String> publicSubnetTypes = new HashSet<>();

                for (AWSZoneConfig zone : region.getZones()) {
                    String zoneName = zone.getName();
                    InstanceResource gatewayInstanceResource = null;
                    NatGatewayResource natGatewayResource = null;
                    boolean natGateway = false;
                    List<RouteTableResource> privateRtResources = new ArrayList<>();

                    for (AWSSubnetConfig subnet : zone.getSubnets()) {
                        if (subnet.getEndpoints() != null) {
                            for (String endPointName : subnet.getEndpoints()) {
                                if (!endpointResourceByName.containsKey(endPointName)) {
                                    throw new BeamException("vpc endpoint " + endPointName + " does not exist.");
                                }

                                VpcEndpointResource endpointResource = endpointResourceByName.get(endPointName);
                                endpointResource.getSubnetSignatures().add(zone.getName() + " " + subnet.getCidr());
                            }
                        }

                        SubnetResource subnetResource = new SubnetResource();

                        vpcResource.getSubnets().add(subnetResource);
                        subnetResource.setAvailabilityZone(zoneName);
                        subnetResource.setCidrBlock(subnet.getCidr());
                        subnetResource.setRegion(awsRegion);

                        // Calculate subnet and route table names for
                        // display in the console.
                        StringBuilder subnetName = new StringBuilder();
                        StringBuilder rtName = new StringBuilder();

                        subnetName.append(project);
                        subnetName.append(" serial-");
                        subnetName.append(getSerial());
                        subnetName.append(" [");

                        rtName.append(project);
                        rtName.append(" serial-");
                        rtName.append(getSerial());
                        rtName.append(' ');

                        // Map subnet types to beam groups.
                        for (String subnetType : subnet.getTypes()) {
                            subnetName.append(subnetType);
                            subnetName.append(", ");
                            rtName.append(subnetType);
                            rtName.append('-');

                            List<SubnetResource> subnetResources = subnetResourcesByType.get(subnetType);

                            if (subnetResources == null) {
                                subnetResources = new ArrayList<>();
                                subnetResourcesByType.put(subnetType, subnetResources);
                            }

                            subnetResources.add(subnetResource);

                            if (subnet.isPublicAccessible()) {
                                publicSubnetTypes.add(subnetType);
                            }
                        }

                        subnetName.setLength(subnetName.length() - 2);
                        subnetName.append(']');
                        putTags(runtime, subnetResource, subnetName.toString());

                        // Skip gateway if excludeLayers are defined and gateway layer is
                        // not included.
                        if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains("gateway")) {
                            continue;
                        }

                        // Routes for the subnet.
                        RouteTableResource rtResource = new RouteTableResource();
                        rtResource.setRegion(awsRegion);

                        subnetResource.setRouteTable(rtResource);
                        rtName.setLength(rtName.length() - 1);
                        putTags(runtime, rtResource, rtName.toString());

                        // Route public subnet through the VPC internet gateway.
                        boolean isSubnetPublic = subnet.isPublicAccessible();
                        subnetResource.setMapPublicIpOnLaunch(isSubnetPublic);

                        if (isSubnetPublic) {
                            RouteResource internetRouteResource = new RouteResource();

                            rtResource.getRoutes().add(internetRouteResource);
                            internetRouteResource.setDestinationCidrBlock("0.0.0.0/0");
                            internetRouteResource.setTarget(internetRouteResource.newReference(igResource));
                            internetRouteResource.setRegion(awsRegion);
                        } else {
                            privateRtResources.add(rtResource);
                        }

                        // NAT gateway?
                        if (subnet.isNatGateway() || subnet.getNatGatewayIp() != null) {
                            natGateway = subnet.isNatGateway();
                            natGatewayResource = new NatGatewayResource();
                            natGatewayResource.setRegion(awsRegion);
                            natGatewayResource.setElasticIp(subnet.getNatGatewayIp());
                            natGatewayResource.setSubnet(natGatewayResource.newReference(subnetResource));
                            subnetResource.getNatGatewayResources().add(natGatewayResource);
                        }

                        // Gateway?
                        GatewayConfig gateway = subnet.getGateway();
                        if (gateway != null) {
                            Image gatewayImage = images.get(gateway.getImage());
                            String gatewayImageId = findImageId(images, gateway.getImage());
                            gatewayInstanceResource = new InstanceResource();
                            gatewayInstanceResource.setRegion(awsRegion);

                            // Verify that the requested Elastic IP is available.
                            if (gateway.getElasticIp() && gateway.getPublicIpAddress() != null) {
                                String allocationId = findElasticIp(ec2Client, gateway.getPublicIpAddress());

                                gatewayInstanceResource.setElasticIpAllocationId(allocationId);
                            }

                            subnetResource.getInstances().add(gatewayInstanceResource);
                            gatewayInstanceResource.setBeamId(UUID.randomUUID().toString());
                            gatewayInstanceResource.setElasticIp(gateway.getElasticIp());
                            gatewayInstanceResource.setIamInstanceProfile(ipReferenceByLayerName.get("gateway"));
                            gatewayInstanceResource.setImageId(gatewayImageId);
                            gatewayInstanceResource.setInstanceType(gateway.getInstanceType());
                            gatewayInstanceResource.setKeyName(keyName);
                            gatewayInstanceResource.setSourceDestCheck(Boolean.FALSE);
                            gatewayInstanceResource.setState(InstanceStateName.Running.toString());
                            gatewayInstanceResource.setBeamLaunchIndex(0);

                            if (gatewayImage == null) {
                                throw new BeamException("Unable to find gateway image: " + gateway.getImage());
                            }

                            // Private and optional public IP addresses.
                            String privateIpAddress = gateway.getIpAddress();

                            if (privateIpAddress != null) {
                                InstanceNetworkInterfaceSpecification ini = new InstanceNetworkInterfaceSpecification();

                                gatewayInstanceResource.getNetworkInterfaces().add(ini);
                                ini.setDeleteOnTermination(Boolean.TRUE);
                                ini.setDeviceIndex(0);
                                ini.setPrivateIpAddress(privateIpAddress);

                                if (isSubnetPublic) {
                                    ini.setAssociatePublicIpAddress(Boolean.TRUE);
                                }
                            }

                            // Tags.
                            gatewayInstanceResource.setUserData(BaseEncoding.base64().encode(ObjectUtils.toJson(new ImmutableMap.Builder<>().
                                    put("project", project).
                                    put("environment", "network").
                                    put("serial", serial).
                                    put("internalDomain", internalDomain).
                                    put("sandbox", sandbox).
                                    put("layer", "gateway").
                                    putAll(gateway.getUserData()).
                                    build()).getBytes(StringUtils.UTF_8)));

                            putTags(runtime, gatewayInstanceResource, String.format("%s %s serial-%s gateway", project, zoneName, serial));
                            gatewayInstanceResource.getTags().put("beam.gateway", "true");
                            gatewayInstanceResource.getTags().put("beam.env", "network");
                            gatewayInstanceResource.getTags().put("beam.layer", "gateway");

                            // Security groups.
                            for (String ruleName : gateway.getSecurityRules()) {
                                SecurityGroupResource sgResource = sgResourceByRuleName.get(ruleName);

                                if (sgResource != null) {
                                    gatewayInstanceResource.getSecurityGroups().add(gatewayInstanceResource.newReference(sgResource));
                                }
                            }

                            // DNS
                            if (gateway.getHostnames().isEmpty()) {
                                gateway.getHostnames().add("vpn." + domainDot);
                            }

                            for (String hostname : gateway.getHostnames()) {
                                if (!hostname.endsWith(".")) {
                                    hostname = hostname + "." + domainDot;
                                }

                                HostedZoneRRSetResource record = gatewayDnsMap.get(hostname);
                                if (record == null) {
                                    record = new HostedZoneRRSetResource();
                                    record.setTtl(60L);
                                    record.setType("A");
                                    record.setName(StringUtils.ensureEnd(hostname, "."));
                                    hzResource.getResourceRecordSets().add(record);

                                    gatewayDnsMap.put(hostname, record);
                                }

                                record.getValues().add(new HostedZoneRRSetResource.ReferenceValue(record.newReference(gatewayInstanceResource)));
                            }

                            for (String hostname : gateway.getPrivateHostnames()) {
                                if (!hostname.endsWith(".")) {
                                    hostname = hostname + "." + privateDomainDot;
                                }

                                HostedZoneRRSetResource record = gatewayPrivateDnsMap.get(hostname);
                                if (record == null) {
                                    record = new HostedZoneRRSetResource();
                                    record.setTtl(60L);
                                    record.setType("A");
                                    record.setName(StringUtils.ensureEnd(hostname, "."));
                                    privateHzResource.getResourceRecordSets().add(record);

                                    gatewayPrivateDnsMap.put(hostname, record);
                                }

                                record.getValues().add(new HostedZoneRRSetResource.ReferenceValue(record.newReference(gatewayInstanceResource), true));
                            }

                            // Copy EBS volumes in Image to EbsResources.
                            Map<String, EbsResource> implicitDevices = new HashMap<>();
                            for (BlockDeviceMapping blockDevice : gatewayImage.getBlockDeviceMappings()) {
                                String volumeName = String.format("gateway %s %s", project, blockDevice.getDeviceName());

                                if (blockDevice.getEbs() == null) {
                                    continue;
                                }

                                EbsResource ebsResource = new EbsResource();
                                ebsResource.setDeleteOnTerminate(blockDevice.getEbs().getDeleteOnTermination());
                                ebsResource.setDeviceName(blockDevice.getDeviceName());
                                ebsResource.setVolumeType(blockDevice.getEbs().getVolumeType());
                                ebsResource.setEncrypted(blockDevice.getEbs().getEncrypted());
                                ebsResource.setIops(blockDevice.getEbs().getIops());
                                ebsResource.setSize(blockDevice.getEbs().getVolumeSize());
                                ebsResource.setName(blockDevice.getVirtualName());
                                ebsResource.setRegion(awsRegion);
                                ebsResource.setCopiedFromImage(true);

                                putTags(runtime, ebsResource, volumeName);
                                ebsResource.getTags().put("beam.env", "network");
                                ebsResource.getTags().put("beam.layer", "gateway");

                                gatewayInstanceResource.getVolumes().add(ebsResource);
                                implicitDevices.put(blockDevice.getDeviceName(), ebsResource);
                            }

                            // EBS Volumes.
                            GATEWAY_VOLUMES: for (VolumeConfig volumeConfig : gateway.getVolumes()) {
                                String volumeName = String.format("gateway %s %s",
                                        project, volumeConfig.getName());

                                EbsResource ebsResource = implicitDevices.get(volumeConfig.getDeviceName());
                                if (ebsResource == null) {
                                    ebsResource = new EbsResource();
                                }

                                ebsResource.setAvailabilityZone(subnetResource.getAvailabilityZone());
                                ebsResource.setDeleteOnTerminate(volumeConfig.getDeleteOnTerminate());
                                ebsResource.setDeviceName(volumeConfig.getDeviceName());
                                ebsResource.setVolumeType(volumeConfig.getVolumeType());
                                ebsResource.setEncrypted(volumeConfig.getEncrypted());
                                ebsResource.setIops(volumeConfig.getIops());
                                ebsResource.setSize(volumeConfig.getSize());
                                ebsResource.setName(volumeName);
                                ebsResource.setRegion(awsRegion);

                                putTags(runtime, ebsResource, volumeName);
                                ebsResource.getTags().put("beam.env", "network");
                                ebsResource.getTags().put("beam.layer", "gateway");

                                if (!implicitDevices.containsKey(volumeConfig.getDeviceName())) {
                                    gatewayInstanceResource.getVolumes().add(ebsResource);
                                }
                            }
                        }
                    }

                    // Route private subnets through the NAT gateway or gateway instance.
                    AWSResource<?> natResource = natGateway ? natGatewayResource : gatewayInstanceResource;
                    if (natResource != null) {
                        for (RouteTableResource rtResource : privateRtResources) {
                            RouteResource internetRouteResource = new RouteResource();

                            rtResource.getRoutes().add(internetRouteResource);
                            internetRouteResource.setDestinationCidrBlock("0.0.0.0/0");
                            internetRouteResource.setTarget(internetRouteResource.newReference(natResource));
                            internetRouteResource.setRegion(awsRegion);
                        }
                    }
                }

                // Load balancers in the VPC.
                Map<String, LoadBalancerResource> loadBalancers = new HashMap<>();

                for (AWSLoadBalancerConfig lb : region.getLoadBalancers()) {
                    LoadBalancerResource lbResource = new LoadBalancerResource();
                    String lbName = lb.getName();
                    String fullName = createLoadBalancerName(runtime, lbName);

                    vpcResource.getLoadBalancers().add(lbResource);
                    loadBalancers.put(lbName, lbResource);
                    lbResource.setCrossZoneLoadBalancing(Boolean.TRUE);
                    lbResource.setLoadBalancerName(fullName);
                    lbResource.setRegion(awsRegion);
                    lbResource.setScheme(lb.getScheme());

                    Integer idleTimeout = lb.getIdleTimeout();
                    lbResource.setIdleTimeout(idleTimeout);

                    putTags(runtime, lbResource, lbName);

                    // Subnets.
                    String subnetType = lb.getSubnetType();
                    List<SubnetResource> subnetResources = subnetResourcesByType.get(subnetType);

                    if (subnetResources == null) {
                        throw new IllegalArgumentException(String.format(
                                "[%s] isn't a valid subnet type!",
                                subnetType));
                    }

                    for (SubnetResource subnetResource : subnetResources) {
                        lbResource.getSubnets().add(lbResource.newReference(subnetResource));
                    }

                    // LB health check.
                    LoadBalancerHealthCheckResource lbhcResource = new LoadBalancerHealthCheckResource();
                    AWSLoadBalancerHealthCheckConfig lbhc = lb.getHealthCheck();

                    lbResource.setHealthCheck(lbhcResource);
                    lbhcResource.setHealthyThreshold(lbhc.getHealthyCount());
                    lbhcResource.setInterval(lbhc.getInterval());
                    lbhcResource.setTarget(String.format("%s:%s%s", lbhc.getProtocol(), lbhc.getPort(), lbhc.getPath()));
                    lbhcResource.setTimeout(lbhc.getTimeout());
                    lbhcResource.setUnhealthyThreshold(lbhc.getUnhealthyCount());
                    lbhcResource.setRegion(awsRegion);

                    // LB listeners.
                    for (AWSLoadBalancerListenerConfig lbl : lb.getListeners()) {
                        LoadBalancerListenerResource lblResource = new LoadBalancerListenerResource();

                        lbResource.getListeners().add(lblResource);
                        lblResource.setInstancePort(lbl.getDestPort());
                        lblResource.setInstanceProtocol(lbl.getDestProtocol());
                        lblResource.setLoadBalancerPort(lbl.getSourcePort());
                        lblResource.setProtocol(lbl.getProtocol());
                        lblResource.setRegion(awsRegion);
                        lblResource.setStickyDuration(lbl.getStickyDuration());

                        String certName = lbl.getSslCertificateName();

                        if (certName != null) {
                            AmazonIdentityManagementClient imClient = new AmazonIdentityManagementClient(getProvider());
                            GetServerCertificateRequest gscRequest = new GetServerCertificateRequest();

                            gscRequest.setServerCertificateName(certName);

                            String certArn = imClient.
                                    getServerCertificate(gscRequest).
                                    getServerCertificate().
                                    getServerCertificateMetadata().
                                    getArn();

                            lblResource.setSslCertificate(lblResource.newReference(ServerCertificateResource.class, certArn));
                        }
                        if (lbl.getCipher() != null) {
                            lblResource.setPredefinedPolicy(lbl.getCipher().getPredefinedPolicy());
                            if (lbl.getCipher().getPredefinedPolicy() == null) {
                                lblResource.setPredefinedPolicy("Custom");
                                lblResource.setSslProtocols(lbl.getCipher().getSslProtocols());
                                lblResource.setSslCiphers(lbl.getCipher().getSslCiphers());
                                lblResource.setServerOrderPreference(lbl.getCipher().getServerOrderPreference());
                            } else if (lbl.getCipher().getServerOrderPreference() != null ||
                                    !lbl.getCipher().getSslCiphers().isEmpty() ||
                                    !lbl.getCipher().getSslProtocols().isEmpty()) {

                                throw new IllegalArgumentException(String.format(
                                        "Cannot customize predefined policy %s for %s!",
                                        lbl.getCipher().getPredefinedPolicy(), lblResource.toDisplayString()));
                            } else {
                                lblResource.setPredefinedPolicyDetails(this);
                            }
                        }
                    }

                    // Security groups.
                    for (String ruleName : lb.getSecurityRules()) {
                        SecurityGroupResource sgResource = sgResourceByRuleName.get(ruleName);

                        if (sgResource != null) {
                            lbResource.getSecurityGroups().add(lbResource.newReference(sgResource));
                        }
                    }

                    // Route 53.
                    AWSLoadBalancerDnsConfig lbDnsConfig = lb.getDns();
                    if (lbDnsConfig != null && lbDnsConfig.getHostnames().size() > 0) {
                        for (String hostname : lbDnsConfig.getHostnames()) {
                            HostedZoneRRSetResource rrSetResource = new HostedZoneRRSetResource();

                            if (!hostname.endsWith(".")) {
                                hostname = hostname + "." + domainDot;
                            }

                            hzResource.getResourceRecordSets().add(rrSetResource);
                            rrSetResource.setName(hostname);
                            rrSetResource.setAliasTarget(rrSetResource.newReference(lbResource));

                            RoutingType routingType = RoutingType.findRouteType(lbDnsConfig.getRoutingPolicy());
                            if (routingType == RoutingType.WEIGHTED) {
                                rrSetResource.setRoutingType(routingType);
                                rrSetResource.setWeight(lbDnsConfig.getWeight());
                            } else if (routingType == RoutingType.LATENCY) {
                                rrSetResource.setRoutingType(routingType);
                                rrSetResource.setResourceRegion(regionName);
                            }
                        }

                        lbResource.setVerificationHostnames(lbDnsConfig.getVerificationHostnames());
                    }
                }

                AmazonS3 s3 = new AmazonS3Client(getProvider());
                // Instances in the VPC by layer.
                LAYERS: for (LayerConfig layer : config.getLayers()) {
                    if (getIncludedLayers().size() > 0 && !getIncludedLayers().contains(layer.getName())) {
                        continue;
                    }

                    Image layerImage = images.get(layer.getImage());
                    String layerImageId = findImageId(images, layer.getImage());
                    String layerName = layer.getName();
                    BeamReference layerInstanceProfile = ipReferenceByLayerName.get(layerName);
                    String layerInstanceType = layer.getInstanceType();

                    // Copy deployment WAR file to S3.
                    DeploymentConfig deployment = layer.getDeployment();
                    String buildPath = null;
                    String buildNumber = null;
                    Map<String, String> dataMap = new HashMap<>();

                    if (deployment != null) {
                        dataMap = deployment.prepare(this, pending);
                        if (dataMap.get("type") != null) {
                            dataMap.putAll(deployment.getGroupHashItems());
                        }
                    }

                    Map userDataMap = new ImmutableMap.Builder<>().
                            put("project", project).
                            put("environment", environment).
                            put("serial", serial).
                            put("internalDomain", internalDomain).
                            put("sandbox", sandbox).
                            put("layer", layerName).
                            put("build", "prod").
                            putAll(layer.getUserData()).
                            putAll(dataMap).build();

                    String userData = BaseEncoding.base64().encode(ObjectUtils.toJson(userDataMap).getBytes(StringUtils.UTF_8));

                    String userDataAutoScale = BaseEncoding.base64().encode(ObjectUtils.toJson(new ImmutableMap.Builder<>().
                            putAll(userDataMap).
                            put("autoscale", "true").
                            build()).getBytes(StringUtils.UTF_8));

                    for (PlacementConfig placement : layer.getPlacements()) {
                        String subnetType = placement.getSubnetType();
                        List<SubnetResource> subnetResources = subnetResourcesByType.get(subnetType);
                        List<String> elasticIpList = new ArrayList<>();

                        if (placement.getElasticIp() && !placement.getElasticIps().isEmpty()) {
                            elasticIpList.addAll(placement.getElasticIps());
                        }

                        if (subnetResources == null) {
                            continue LAYERS;
                        }

                        if (layerImage == null) {
                            throw new BeamException("Unable to find image: " + layer.getImage());
                        }

                        AutoScaleConfig as = placement.getAutoscale();
                        int subnetResourcesSize = subnetResources.size();

                        // Static number of instances.
                        if (as == null) {
                            int placementSizePerSubnet = placement.getSizePerSubnet();

                            // Configure hostnames.
                            List<HostedZoneRRSetResource> hostnameResources = new ArrayList<>();
                            if (placement.getHostnames().size() > 0) {
                                for (String hostname : placement.getHostnames()) {
                                    HostedZoneRRSetResource rrSetResource = new HostedZoneRRSetResource();

                                    if (!hostname.endsWith(".")) {
                                        hostname = hostname + "." + domainDot;
                                    }

                                    hzResource.getResourceRecordSets().add(rrSetResource);
                                    rrSetResource.setName(hostname);
                                    rrSetResource.setTtl(60L);
                                    rrSetResource.setType("CNAME");

                                    if (placementSizePerSubnet > 1) {
                                        rrSetResource.setType("A");
                                    }

                                    hostnameResources.add(rrSetResource);
                                }
                            }

                            List<HostedZoneRRSetResource> privateHostnameResources = new ArrayList<>();
                            for (String hostname : placement.getPrivateHostnames()) {
                                if (!hostname.endsWith(".")) {
                                    hostname = hostname + "." + privateDomainDot;
                                }

                                HostedZoneRRSetResource record = layerPrivateDnsMap.get(hostname);
                                if (record == null) {
                                    record = new HostedZoneRRSetResource();
                                    record.setTtl(60L);
                                    record.setType("A");
                                    record.setName(StringUtils.ensureEnd(hostname, "."));
                                    privateHzResource.getResourceRecordSets().add(record);

                                    layerPrivateDnsMap.put(hostname, record);
                                    privateHostnameResources.add(record);
                                }
                            }

                            int subnetIndex = 0;
                            for (SubnetResource subnetResource : subnetResources) {
                                Integer beamLaunchIndex = 0;

                                for (int i = 0; i < placementSizePerSubnet; i++) {
                                    InstanceResource iResource = new InstanceResource();

                                    subnetResource.getInstances().add(iResource);
                                    iResource.setIamInstanceProfile(layerInstanceProfile);
                                    iResource.setImageId(layerImageId);
                                    iResource.setInstanceType(layerInstanceType);
                                    iResource.setKeyName(keyName);
                                    iResource.setState(InstanceStateName.Running.toString());
                                    iResource.setUserData(userData);

                                    // Verify that the requested Elastic IP is available.
                                    int elasticIpIndex = subnetIndex * placementSizePerSubnet + i;
                                    if (placement.getElasticIp() && elasticIpIndex < elasticIpList.size()) {
                                        String allocationId = findElasticIp(ec2Client, elasticIpList.get(elasticIpIndex));

                                        iResource.setElasticIpAllocationId(allocationId);
                                    }

                                    iResource.setElasticIp(placement.getElasticIp());
                                    iResource.setRegion(awsRegion);
                                    iResource.setBeamLaunchIndex(beamLaunchIndex++);

                                    putTags(runtime, iResource, String.format("%s %s serial-%s %s", project, environment, serial, layerName));
                                    iResource.getTags().put("beam.env", environment);
                                    iResource.getTags().put("beam.layer", layerName);

                                    // Security groups.
                                    for (String ruleName : layer.getSecurityRules()) {
                                        SecurityGroupResource sgResource = sgResourceByRuleName.get(ruleName);

                                        if (sgResource != null) {
                                            iResource.getSecurityGroups().add(iResource.newReference(sgResource));
                                        }
                                    }

                                    for (HostedZoneRRSetResource rrSetResource : hostnameResources) {

                                        HostedZoneRRSetResource.ReferenceValue value;
                                        if (subnetResource.getMapPublicIpOnLaunch() != null &&
                                                subnetResource.getMapPublicIpOnLaunch()) {
                                            value = new HostedZoneRRSetResource.ReferenceValue(rrSetResource.newReference(iResource));

                                        } else {
                                            value = new HostedZoneRRSetResource.ReferenceValue(rrSetResource.newReference(iResource), true);
                                        }

                                        if (subnetResource.getMapPublicIpOnLaunch() && placementSizePerSubnet <= 1) {
                                            value.setType("CNAME");
                                        } else {
                                            value.setType("A");
                                        }

                                        rrSetResource.getValues().add(value);
                                    }

                                    if (subnetResource.getMapPublicIpOnLaunch() != null &&
                                            !subnetResource.getMapPublicIpOnLaunch()) {
                                        for (HostedZoneRRSetResource rrSetResource : hostnameResources) {
                                            rrSetResource.setType("A");
                                        }
                                    }

                                    for (HostedZoneRRSetResource record : privateHostnameResources) {
                                        record.getValues().add(new HostedZoneRRSetResource.ReferenceValue(record.newReference(iResource), true));
                                    }

                                    // Copy EBS volumes in Image to EbsResources.
                                    Map<String, EbsResource> implicitDevices = new HashMap<>();
                                    for (BlockDeviceMapping blockDevice : layerImage.getBlockDeviceMappings()) {
                                        String volumeName = String.format("%s %s %s", layerName, project, blockDevice.getDeviceName());

                                        if (blockDevice.getEbs() == null) {
                                            continue;
                                        }

                                        EbsResource ebsResource = new EbsResource();
                                        ebsResource.setDeleteOnTerminate(blockDevice.getEbs().getDeleteOnTermination());
                                        ebsResource.setDeviceName(blockDevice.getDeviceName());
                                        ebsResource.setVolumeType(blockDevice.getEbs().getVolumeType());
                                        ebsResource.setEncrypted(blockDevice.getEbs().getEncrypted());
                                        ebsResource.setIops(blockDevice.getEbs().getIops());
                                        ebsResource.setSize(blockDevice.getEbs().getVolumeSize());
                                        ebsResource.setName(blockDevice.getVirtualName());
                                        ebsResource.setRegion(awsRegion);
                                        ebsResource.setCopiedFromImage(true);

                                        putTags(runtime, ebsResource, volumeName);
                                        ebsResource.getTags().put("beam.env", environment);
                                        ebsResource.getTags().put("beam.layer", layerName);

                                        iResource.getVolumes().add(ebsResource);
                                        implicitDevices.put(blockDevice.getDeviceName(), ebsResource);
                                    }

                                    // EBS Volumes.
                                    INSTANCE_VOLUMES: for (VolumeConfig volumeConfig : layer.getVolumes()) {
                                        String volumeName = String.format("%s %s %s",
                                                layerName, project, volumeConfig.getName());

                                        EbsResource ebsResource = implicitDevices.get(volumeConfig.getDeviceName());
                                        if (ebsResource == null) {
                                            ebsResource = new EbsResource();
                                        }

                                        ebsResource.setAvailabilityZone(subnetResource.getAvailabilityZone());
                                        ebsResource.setDeleteOnTerminate(volumeConfig.getDeleteOnTerminate());
                                        ebsResource.setDeviceName(volumeConfig.getDeviceName());
                                        ebsResource.setVolumeType(volumeConfig.getVolumeType());
                                        ebsResource.setEncrypted(volumeConfig.getEncrypted());
                                        ebsResource.setIops(volumeConfig.getIops());
                                        ebsResource.setSize(volumeConfig.getSize());
                                        ebsResource.setName(volumeName);
                                        ebsResource.setRegion(awsRegion);
                                        ebsResource.setCopiedFromImage(false);

                                        putTags(runtime, ebsResource, volumeName);
                                        ebsResource.getTags().put("beam.env", environment);
                                        ebsResource.getTags().put("beam.layer", layerName);

                                        if (!implicitDevices.containsKey(volumeConfig.getDeviceName())) {
                                            iResource.getVolumes().add(ebsResource);
                                        }
                                    }
                                }

                                subnetIndex++;
                            }

                        // Auto scaled instances.
                        } else {
                            AutoScalingGroupResource asgResource = new AutoScalingGroupResource();

                            vpcResource.getAutoScalingGroups().add(asgResource);
                            asgResource.setHealthCheckGracePeriod(300);
                            asgResource.setMaxSize(as.getMaxPerSubnet() * subnetResourcesSize);
                            asgResource.setMinSize(as.getMinPerSubnet() * subnetResourcesSize);
                            asgResource.setRegion(awsRegion);
                            asgResource.setHostedZone(asgResource.newReference(hzResource));

                            // Subnets.
                            for (SubnetResource subnetResource : subnetResources) {
                                asgResource.getSubnets().add(asgResource.newReference(subnetResource));
                            }

                            // Load balancer.
                            List<String> lbs = as.getLoadBalancers();
                            StringBuilder hashBuilder = new StringBuilder();

                            if (lbs != null && !lbs.isEmpty()) {
                                for (String lb : lbs) {
                                    BeamReference lbReference = asgResource.newReference(loadBalancers.get(lb));

                                    if (lbReference != null) {
                                        asgResource.getLoadBalancers().add(lbReference);
                                    } else {
                                        throw new BeamException(String.format("Elb %s does not exist.", lb));
                                    }
                                }

                            } else {
                                String lb = as.getLoadBalancer();

                                if (lb != null) {
                                    BeamReference lbReference = asgResource.newReference(loadBalancers.get(lb));

                                    if (lbReference != null) {
                                        asgResource.getLoadBalancers().add(lbReference);
                                    } else {
                                        throw new BeamException(String.format("Elb %s does not exist.", lb));
                                    }
                                }
                            }

                            // One launch config per auto scale group resource.
                            LaunchConfigurationResource lcResource = new LaunchConfigurationResource();

                            vpcResource.getLaunchConfigurations().add(lcResource);
                            lcResource.setAssociatePublicIpAddress(appendHash(hashBuilder, "associatePublichIpAddress", publicSubnetTypes.contains(subnetType)));
                            lcResource.setIamInstanceProfile(appendHash(hashBuilder, "iamInstanceProfile", layerInstanceProfile));
                            lcResource.setImageId(appendHash(hashBuilder, "imageId", layerImageId));
                            lcResource.setImageName(layer.getImage());
                            lcResource.setInstanceType(appendHash(hashBuilder, "instanceType", layerInstanceType));
                            lcResource.setKeyName(appendHash(hashBuilder, "keyName", keyName));
                            lcResource.setUserData(userDataAutoScale);
                            lcResource.setRegion(awsRegion);

                            // Append the userData without the "autoscale" key to prevent the hash
                            // from changing from previous versions of Beam which would force
                            // a redeploy of this layer.
                            appendHash(hashBuilder, "userData", userData);

                            // Launch config security groups.
                            for (String ruleName : layer.getSecurityRules()) {
                                SecurityGroupResource sgResource = sgResourceByRuleName.get(ruleName);

                                appendHash(hashBuilder, "securityGroups", ruleName);

                                if (sgResource != null) {
                                    lcResource.getSecurityGroups().add(lcResource.newReference(sgResource));
                                }
                            }

                            // Unique launch config name based on its parameters.
                            String hash = StringUtils.hex(StringUtils.md5(hashBuilder.toString()));
                            String asName;

                            if (deployment == null) {
                                asName = String.format("%s %s %s v%s %s %s", project, layerName, environment, serial, layerImageId, hash.substring(0, 8));

                            } else {
                                Map<String, String> groupHashItems = deployment.getGroupHashItems();

                                if (groupHashItems.containsKey("buildNumber")) {
                                    buildNumber = groupHashItems.get("buildNumber");
                                } else {
                                    buildNumber = "";
                                }

                                if (groupHashItems.containsKey("jenkinsBuildPath")) {
                                    buildPath = groupHashItems.get("jenkinsBuildPath");
                                } else {
                                    buildPath = "";
                                }

                                asName = String.format("%s %s %s v%s %s %s %s %s", project, layerName, environment, serial, layerImageId, buildPath, buildNumber, hash);
                            }

                            asgResource.setAutoScalingGroupName(asName);
                            asgResource.setLaunchConfiguration(asgResource.newReference(lcResource));
                            lcResource.setLaunchConfigurationName(asName);

                            StringBuilder groupHashBuilder = new StringBuilder();
                            appendHash(groupHashBuilder, "image", layer.getImage());

                            if (deployment != null) {
                                Map<String, String> groupHashItems = deployment.getGroupHashItems();
                                for (String key : groupHashItems.keySet()) {
                                    String value = groupHashItems.get(key);
                                    appendHash(groupHashBuilder, key, value);
                                }

                                asgResource.setDeployment(deployment);
                            }

                            String groupHash = StringUtils.hex(StringUtils.md5(groupHashBuilder.toString()));
                            asgResource.setGroupHash(groupHash);

                            // Auto scaling group policies.
                            for (AutoScalePolicyConfig policy : as.getPolicies()) {
                                String policyName = project + "-" + policy.getName() + "-" + hash;
                                AutoScalingGroupPolicyResource policyResource = new AutoScalingGroupPolicyResource();

                                asgResource.getPolicies().add(policyResource);
                                policyResource.setAdjustmentType("ChangeInCapacity");
                                policyResource.setCooldown(policy.getCooldown());
                                policyResource.setPolicyName(policyName);
                                policyResource.setScalingAdjustment(policy.getInstancesPerSubnet() * subnetResourcesSize);
                                policyResource.setRegion(awsRegion);

                                // Alarm.
                                AutoScalePolicyAlarmConfig alarm = policy.getAlarm();
                                MetricAlarmResource alarmResource = new MetricAlarmResource();

                                policyResource.setMetricAlarm(alarmResource);
                                alarmResource.setAlarmName(policyName);
                                alarmResource.getDimensions().put("AutoScalingGroupName", asName);
                                alarmResource.setMetricName("CPUUtilization");
                                alarmResource.setNamespace("AWS/EC2");
                                alarmResource.setEvaluationPeriods(1);
                                alarmResource.setPeriod(alarm.getPeriod());
                                alarmResource.setStatistic("Average");
                                alarmResource.setRegion(awsRegion);

                                double greater = alarm.getGreater();

                                if (greater > 0) {
                                    alarmResource.setComparisonOperator("GreaterThanThreshold");
                                    alarmResource.setThreshold(greater);

                                } else {
                                    alarmResource.setComparisonOperator("LessThanThreshold");
                                    alarmResource.setThreshold(alarm.getLess());
                                }
                            }

                            // Schedules.
                            List<AutoScalingGroupScheduleResource> schedules = new ArrayList<>();
                            for (AutoScaleScheduleConfig schedule : as.getSchedules()) {
                                Integer scaleUp = null;
                                if (schedule.getScaleUpPerSubnet() != null) {
                                    scaleUp = schedule.getScaleUpPerSubnet() * subnetResourcesSize;
                                }

                                Integer scaleDown = null;
                                if (schedule.getScaleDownPerSubnet() != null) {
                                    scaleDown = schedule.getScaleDownPerSubnet() * subnetResourcesSize;
                                }

                                Integer scaleDesired = null;
                                if (schedule.getDesiredPerSubnet() != null) {
                                    scaleDesired = schedule.getDesiredPerSubnet() * subnetResourcesSize;
                                }

                                if (scaleDesired != null) {
                                    AutoScalingGroupScheduleResource startAction = new AutoScalingGroupScheduleResource();
                                    startAction.setName(schedule.getName());
                                    startAction.setRecurrence(schedule.getStartRecurrence());
                                    startAction.setDesired(scaleDesired);
                                    startAction.setRegion(awsRegion);
                                    startAction.setStartTime(schedule.getStartDateTime() != null ? schedule.getStartDateTime().toDate() : null);
                                    if (schedule.isRecurring() && schedule.getEndTime() != null) {
                                        startAction.setEndTime(schedule.getEndDateTime().toDate());
                                    }
                                    schedules.add(startAction);
                                }

                                if (scaleUp != null) {
                                    AutoScalingGroupScheduleResource startAction = new AutoScalingGroupScheduleResource();
                                    startAction.setName(schedule.getName() + " start");
                                    startAction.setRecurrence(schedule.getStartRecurrence());
                                    startAction.setMin(scaleUp);
                                    startAction.setRegion(awsRegion);
                                    startAction.setStartTime(schedule.getStartDateTime() != null ? schedule.getStartDateTime().toDate() : null);
                                    if (schedule.isRecurring() && schedule.getEndTime() != null) {
                                        startAction.setEndTime(schedule.getEndDateTime().toDate());
                                    }
                                    schedules.add(startAction);
                                }

                                if (scaleDown != null) {
                                    AutoScalingGroupScheduleResource endAction = new AutoScalingGroupScheduleResource();
                                    endAction.setName(schedule.getName() + " end");
                                    endAction.setRecurrence(schedule.getEndRecurrence());
                                    endAction.setMin(scaleDown);
                                    endAction.setRegion(awsRegion);
                                    endAction.setStartTime(schedule.getStartDateTime() != null ? schedule.getStartDateTime().plusMinutes(1).toDate() : null);
                                    if (!schedule.isRecurring() && schedule.getEndDateTime() != null) {
                                        endAction.setStartTime(schedule.getEndDateTime().toDate());
                                    }
                                    if (schedule.isRecurring() && schedule.getEndTime() != null) {
                                        endAction.setEndTime(schedule.getEndDateTime().toDate());
                                    }
                                    schedules.add(endAction);
                                }
                            }

                            if (!schedules.isEmpty()) {
                                asgResource.setSchedules(schedules);
                            }

                            // Tags.
                            addAutoScalingGroupTag(asgResource, "Name", asName);
                            addAutoScalingGroupTag(asgResource, "Project", project);
                            addAutoScalingGroupTag(asgResource, "beam.project", project);
                            addAutoScalingGroupTag(asgResource, "beam.env", environment);
                            addAutoScalingGroupTag(asgResource, "beam.serial", serial);
                            addAutoScalingGroupTag(asgResource, "beam.layer", layerName);
                        }
                    }
                }
            }

            if (privateHzResource.getResourceRecordSets().size() > 0) {
                pending.getHostedZones().add(privateHzResource);
            }

            BeamResource.updateTree(current);
            BeamResource.updateTree(pending);

            List<Diff<?, ?, ?>> diffs = new ArrayList<>();

            ResourceDiff bucketDiff = new ResourceDiff(
                    this,
                    pending.getFilter(),
                    BucketResource.class,
                    current.getBuckets(),
                    pending.getBuckets());

            bucketDiff.diff();
            bucketDiff.getChanges().clear();
            bucketDiff.diff();
            diffs.add(bucketDiff);

            ResourceDiff vpcDiff = new ResourceDiff(
                    this,
                    pending.getFilter(),
                    VpcResource.class,
                    current.getVpcs(),
                    pending.getVpcs());

            vpcDiff.diff();
            vpcDiff.getChanges().clear();
            vpcDiff.diff();
            diffs.add(vpcDiff);

            // get changes in auto scaling groups
            List<Change<?>> changes = new ArrayList<>();
            findChangesByResourceClass(diffs, AutoScalingGroupResource.class, changes);
            findChangesByType(changes, ChangeType.CREATE);

            prepareDeploymentResources(changes, current, pending);

            if (!"network".equals(runtime.getEnvironment())) {
                ResourceDiff deploymentDiffs = new ResourceDiff(
                        this,
                        pending.getFilter(),
                        DeploymentResource.class,
                        current.getDeployments(),
                        pending.getDeployments());

                deploymentDiffs.diff();
                deploymentDiffs.getChanges().clear();
                deploymentDiffs.diff();
                diffs.add(deploymentDiffs);
            }

            ResourceDiff hostedZoneDiff = new ResourceDiff(
                    this,
                    pending.getFilter(),
                    HostedZoneResource.class,
                    current.getHostedZones(),
                    pending.getHostedZones());

            hostedZoneDiff.diff();
            hostedZoneDiff.getChanges().clear();
            hostedZoneDiff.diff();
            diffs.add(hostedZoneDiff);

            tryToKeep(diffs);
            return diffs;
        }

        return null;
    }

    private void findChangesByResourceClass(List<Diff<?, ?, ?>> diffs, Class<?> resourceClass, List<Change<?>> changes) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                BeamResource resource = ((ResourceChange)change).getResource();
                if (resource.getClass().equals(resourceClass)) {
                    changes.add(change);
                } else {
                    findChangesByResourceClass(change.getDiffs(), resourceClass, changes);
                }
            }
        }
    }

    private void findChangesByType(List<Change<?>> changes, ChangeType type) {
        Iterator<Change<?>> iter = changes.iterator();
        while (iter.hasNext()) {
            Change<?> change = iter.next();
            if (change.getType() != type) {
                iter.remove();
            }
        }
    }

    private void prepareDeploymentResources(List<Change<?>> changes, AWSCloudConfig current, AWSCloudConfig pending) {
        Map<String, beam.aws.config.DeploymentResource> deploymentResources = new LinkedHashMap<>();
        for (DeploymentResource currentDeployment : current.getDeployments()) {
            deploymentResources.put(currentDeployment.getGroupHash(), currentDeployment);
        }

        current.getDeployments().clear();

        for(Change<?> change : changes) {
            BeamResource resource = ((ResourceChange)change).getResource();
            AutoScalingGroupResource asgResource = (AutoScalingGroupResource)resource;

            DeploymentResource deploymentResource = deploymentResources.get(asgResource.getGroupHash());
            if (deploymentResource == null) {
                deploymentResource = new DeploymentResource();
                deploymentResource.setGroupHash(asgResource.getGroupHash());

                LaunchConfigurationResource config = (LaunchConfigurationResource)asgResource.getLaunchConfiguration().resolve();
                DeploymentConfig deployment = asgResource.getDeployment();

                deploymentResource.setImage(config.getImageName());
                deploymentResource.setInstanceType(config.getInstanceType());

                if (deployment != null) {
                    deploymentResource.setDeploymentString(deployment.toDisplayString());
                }

                deploymentResources.put(asgResource.getGroupHash(), deploymentResource);
            }

            deploymentResource.getAutoscaleGroups().add(asgResource);
        }

        for (DeploymentResource deploymentResource : deploymentResources.values()) {
            pending.getDeployments().add(deploymentResource);
            current.getDeployments().add(deploymentResource);
        }
    }

    private void addImageIdOrName(Set<String> imageIds, Set<String> imageNames, String image) {
        if (image != null) {
            if (image.startsWith("ami-")) {
                imageIds.add(image);

            } else {
                imageNames.add(image);
            }
        }
    }

    private void populateImages(Map<String, Image> images, AmazonEC2Client client, DescribeImagesRequest diRequest) {
        for (Image image : client.
                describeImages(diRequest).
                getImages()) {

            images.put(image.getImageId(), image);
            images.put(image.getName(), image);
        }
    }

    private String findImageId(Map<String, Image> images, String imageIdOrName) {
        Image image = images.get(imageIdOrName);

        if (image != null) {
            return image.getImageId();

        } else {
            return imageIdOrName;
        }
    }

    private String findElasticIp(AmazonEC2Client client, String publicIpAddress) {
        DescribeAddressesRequest daRequest = new DescribeAddressesRequest();

        Filter filter = new Filter();
        filter.setName("public-ip");
        filter.setValues(Arrays.asList(publicIpAddress));

        daRequest.setFilters(Arrays.asList(filter));

        DescribeAddressesResult daResult = client.describeAddresses(daRequest);

        for (Address address : daResult.getAddresses()) {
            if (address.getAssociationId() == null) {
                return address.getAllocationId();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void createRole(
            VpcResource vpcResource,
            Map<String, BeamReference> ipReferenceByLayerName,
            String layerName,
            String roleName,
            Set<String> policyNames)
            throws IOException {

        // For backward compatibility when roles were created
        // manually and specified by name.
        if (policyNames.isEmpty()) {
            String name = ObjectUtils.firstNonNull(roleName, layerName);
            GetInstanceProfileRequest gipRequest = new GetInstanceProfileRequest().withInstanceProfileName(name);
            AmazonIdentityManagementClient imClient = new AmazonIdentityManagementClient(getProvider());
            imClient.setRegion(getDefaultAWSRegion());

            try {
                imClient.getInstanceProfile(gipRequest);
                ipReferenceByLayerName.put(layerName, vpcResource.newReference(InstanceProfileResource.class, name));
            } catch (AmazonServiceException error) {

            }

        // Create an unique role for the layer.
        } else {
            BeamRuntime runtime = BeamRuntime.getCurrentRuntime();
            RoleResource roleResource = new RoleResource();

            if (roleName == null && !"gateway".equals(layerName)) {
                roleName = String.format("%s-%s-%s-%s", runtime.getProject(), runtime.getEnvironment(), runtime.getSerial(), layerName);
            } else if (roleName == null && "gateway".equals(layerName)) {
                roleName = String.format("%s-network-%s-%s", runtime.getProject(), runtime.getSerial(), layerName);
            }

            vpcResource.getRoles().add(roleResource);
            roleResource.setAssumeRolePolicyDocument((Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(getClass().getResourceAsStream("/ec2-trust-policy.json"), Charsets.UTF_8)));
            roleResource.setRoleName(roleName);

            InstanceProfileResource ipResource = new InstanceProfileResource();

            vpcResource.getInstanceProfiles().add(ipResource);
            ipReferenceByLayerName.put(layerName, vpcResource.newReference(ipResource));
            ipResource.setInstanceProfileName(roleName);
            ipResource.getRoles().add(ipResource.newReference(roleResource));

            for (String policyName : policyNames) {
                RolePolicyResource policyResource = new RolePolicyResource();

                roleResource.getPolicies().add(policyResource);
                policyResource.setPolicyName(policyName);
                policyResource.setPolicyDocumentFile(new File("role-policies/" + policyName + ".json"));
            }
        }
    }

    private void putTags(BeamRuntime runtime, Taggable taggable, String name) {
        Map<String, String> tags = taggable.getTags();
        String project = runtime.getProject();

        tags.put("Name", name);
        tags.put("Project", project);
        tags.put("beam.project", project);
        tags.put("beam.serial", runtime.getSerial());
    }

    private String createLoadBalancerName(BeamRuntime runtime, String name) {
        String project = StringUtils.toHyphenated(runtime.getProject());
        String suffix = StringUtils.toHyphenated(("-" + name + "-" + runtime.getSerial()).toLowerCase(Locale.ENGLISH));
        int max = 30 - suffix.length();

        if (project.length() > max) {
            project = project.substring(0, max);
        }

        return project + suffix;
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
    }

    private void addAutoScalingGroupTag(AutoScalingGroupResource asgResource, String key, String value) {
        AutoScalingGroupTagResource tag = new AutoScalingGroupTagResource();

        tag.setKey(key);
        tag.setPropagateAtLaunch(Boolean.TRUE);
        tag.setValue(value);
        tag.setRegion(asgResource.getRegion());
        asgResource.getTags().add(tag);
    }

    private void tryToKeep(List<Diff<?, ?, ?>> diffs) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                if (change instanceof ResourceUpdate) {
                    ((ResourceUpdate) change).tryToKeep();
                }

                tryToKeep(change.getDiffs());
            }
        }
    }

    private com.amazonaws.regions.Region getDefaultAWSRegion() {
        return RegionUtils.getRegion(getDefaultRegion());
    }

    private String getDefaultRegion() {
        if (defaultRegion != null) {
            return defaultRegion;
        }

        String region = "us-east-1";
        File ec2ConfigPath = new File(System.getProperty("user.home") +  File.separator + ".beam/ec2.yml");
        if (ec2ConfigPath.exists()) {
            Yaml yaml = new Yaml();

            try {
                Map map = (Map) yaml.load(new FileInputStream(ec2ConfigPath));

                if (map != null) {
                    region = (String) map.get("defaultRegion");
                }
            } catch (FileNotFoundException fnfe) {

            }
        }

        defaultRegion = region;

        return defaultRegion;
    }

    private String getS3StandardEndPoint() {
        if (s3StandardEndPoint != null) {
            return s3StandardEndPoint;
        }

        String endpoint = "s3.amazonaws.com";
        File ec2ConfigPath = new File(System.getProperty("user.home") +  File.separator + ".beam/ec2.yml");
        if (ec2ConfigPath.exists()) {
            Yaml yaml = new Yaml();

            try {
                Map map = (Map) yaml.load(new FileInputStream(ec2ConfigPath));

                if (map != null) {
                    endpoint = (String) map.get("s3StandardEndPoint");
                }
            } catch (FileNotFoundException fnfe) {

            }
        }

        s3StandardEndPoint = endpoint;

        return s3StandardEndPoint;
    }

    @Override
    public InetAddress findGateway(BeamRuntime runtime) {
        RootConfig config = runtime.getConfig();

        if (config != null) {
            for (CloudConfig cloudConfig : config.getNetworkConfig().getClouds()) {
                if (cloudConfig instanceof AWSCloudConfig) {
                    for (AWSRegionConfig regionConfig : ((AWSCloudConfig) cloudConfig).getRegions()) {
                        for (AWSZoneConfig zoneConfig : regionConfig.getZones()) {
                            for (SubnetConfig subnet : zoneConfig.getSubnets()) {
                                GatewayConfig gatewayConfig = subnet.getGateway();

                                if (gatewayConfig == null) {
                                    continue;
                                }

                                try {
                                    InetAddress gatewayAddress = InetAddress.getByName(gatewayConfig.getIpAddress());

                                    if (gatewayAddress.isReachable(1000)) {
                                        return gatewayAddress;
                                    }

                                } catch (IOException error) {
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        AWSCredentials creds;

        try {
            AWSCredentialsProvider provider = getProvider();
            if (refresh) {
                provider.refresh();
            }

            creds = provider.getCredentials();
        } catch (AmazonClientException ace) {
            throw new BeamException(ace.getMessage(), null, "no-account");
        }

        mapBuilder.put("accessKeyId", creds.getAWSAccessKeyId());
        mapBuilder.put("secretKey", creds.getAWSSecretKey());

        if (creds instanceof AWSSessionCredentials) {
            mapBuilder.put("sessionToken", ((AWSSessionCredentials) creds).getSessionToken());
        }

        Long expiration = DateTime.now().plusDays(1).getMillis();
        if (creds instanceof EnterpriseCredentials) {
            expiration = ((EnterpriseCredentials) creds).getExpiration();
        }

        mapBuilder.put("expiration", Long.toString(expiration));

        return mapBuilder.build();
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        AWSCredentials creds;

        try {
            AWSCredentialsProvider provider = getProvider();

            if (provider instanceof EnterpriseCredentialsProviderChain) {
                for (AWSCredentialsProvider oneProvider : ((EnterpriseCredentialsProviderChain) provider).getProviders()) {
                    if (oneProvider instanceof EnterpriseCredentialsProvider) {
                        ((EnterpriseCredentialsProvider) oneProvider).setExtended(extended);
                    }
                }
            }

            if (refresh) {
                provider.refresh();
            }

            creds = provider.getCredentials();
        } catch (AmazonClientException ace) {
            throw new BeamException(ace.getMessage(), null, "no-account");
        }

        mapBuilder.put("accessKeyId", creds.getAWSAccessKeyId());
        mapBuilder.put("secretKey", creds.getAWSSecretKey());

        if (creds instanceof AWSSessionCredentials) {
            mapBuilder.put("sessionToken", ((AWSSessionCredentials) creds).getSessionToken());
        }

        Long expiration = DateTime.now().plusDays(1).getMillis();
        if (creds instanceof EnterpriseCredentials) {
            expiration = ((EnterpriseCredentials) creds).getExpiration();
        }

        mapBuilder.put("expiration", Long.toString(expiration));

        return mapBuilder.build();
    }


    public boolean keyExists(String bucket, String key, AmazonS3 s3) {
        if (!s3.doesBucketExist(bucket)) {
            return false;
        }
        try {
            S3Object object = s3.getObject(bucket, key);
        } catch (AmazonServiceException e) {

            return false;
        }
        return true;
    }

    @Override
    public String copyDeploymentFile(String bucketName, String bucketRegion, String buildsKey, String oldBuildsKey, String commonKey, Object pending) {
        AmazonS3 s3 = new AmazonS3Client(getProvider());
        boolean oldLocationHasBuild = keyExists(findOldDefaultBucketName(), oldBuildsKey, s3);
        String jenkinsBucket = bucketName;

        if (oldLocationHasBuild) {
            bucketName = findOldDefaultBucketName();
            buildsKey = oldBuildsKey;
        }

        BucketResource bucketResource = null;
        for (BucketResource b : ((AWSCloudConfig)pending).getBuckets()) {
            if (b.getName().equals(bucketName)) {
                bucketResource = b;

                break;
            }
        }

        if (bucketResource == null) {
            bucketResource = new BucketResource();

            ((AWSCloudConfig)pending).getBuckets().add(bucketResource);
            bucketResource.setName(bucketName);
        }

        S3ObjectResource jenkinsWarResource = new S3ObjectResource();
        jenkinsWarResource.setKey(commonKey);
        jenkinsWarResource.setBucket(jenkinsWarResource.newReference(bucketResource));
        jenkinsWarResource = (S3ObjectResource) jenkinsWarResource.findCurrent(this, null);

        if (jenkinsWarResource != null && jenkinsWarResource.getEtag().matches("^[a-f0-9]{32}-[0-9]+$")) {
            jenkinsWarResource.setObjectContentUrl(String.format("s3://%s/%s", jenkinsBucket, commonKey));
            jenkinsWarResource.create(this);
            jenkinsWarResource = (S3ObjectResource) jenkinsWarResource.findCurrent(this, null);
        }

        S3ObjectResource productionWarResource = new S3ObjectResource();
        productionWarResource.setKey(buildsKey);
        productionWarResource.setBucket(productionWarResource.newReference(bucketResource));
        productionWarResource = (S3ObjectResource) productionWarResource.findCurrent(this, null);

        if (productionWarResource != null && jenkinsWarResource != null && !productionWarResource.getEtag().equals(jenkinsWarResource.getEtag())) {
            throw new BeamException(String.format("Warfile from path '%s' already exists at '%s' and the checksum does not match!",
                    commonKey,
                    buildsKey));
        }

        S3ObjectResource warResource = new S3ObjectResource();

        bucketResource.getS3Objects().add(warResource);
        warResource.setKey(buildsKey);
        warResource.setObjectContentUrl(String.format("s3://%s/%s", jenkinsBucket, commonKey));

        return String.format("s3://%s/%s", bucketName, buildsKey);
    }

    @Override
    public void consoleLogin(boolean readonly, boolean urlOnly, PrintWriter out) throws Exception {
        AWSCredentialsProvider provider = getProvider();
        AWSCredentials creds = provider.getCredentials();

        String accessKeyId;
        String secretAccessKey;
        String sessionToken;

        if (creds instanceof AWSSessionCredentials) {
            AWSSessionCredentials sessionCreds = (AWSSessionCredentials) creds;

            accessKeyId = sessionCreds.getAWSAccessKeyId();
            secretAccessKey = sessionCreds.getAWSSecretKey();
            sessionToken = sessionCreds.getSessionToken();

        } else {
            AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient(provider);
            GetFederationTokenRequest tokenRequest = new GetFederationTokenRequest();

            String role = readonly ? "/readonly-role.json" : "/full-role.json";
            String policy = com.amazonaws.util.IOUtils.toString(ConsoleCommand.class.getResourceAsStream(role));

            tokenRequest.setPolicy(policy);
            tokenRequest.withDurationSeconds(3600);
            tokenRequest.withName(System.getProperty("user.name") + "@beam");

            GetFederationTokenResult tokenResult = client.getFederationToken(tokenRequest);

            Credentials federatedCredentials = tokenResult.getCredentials();

            accessKeyId = federatedCredentials.getAccessKeyId();
            secretAccessKey = federatedCredentials.getSecretAccessKey();
            sessionToken = federatedCredentials.getSessionToken();
        }

        Map<String, String> session = new HashMap<>();
        session.put("sessionId", accessKeyId);
        session.put("sessionKey", secretAccessKey);
        session.put("sessionToken", sessionToken);

        String signInUrl = "https://signin.aws.amazon.com/federation";
        String tokenUrl = StringUtils.addQueryParameters(signInUrl,
                "Action", "getSigninToken",
                "SessionType", "json",
                "Session", ObjectUtils.toJson(session));

        JSONObject tokenResponse = new Resty(Resty.Option.timeout(2000)).
                json(tokenUrl).
                object();

        String consoleUrl = "https://console.aws.amazon.com/ec2";
        String loginUrl = StringUtils.addQueryParameters(signInUrl,
                "SigninToken", tokenResponse.getString("SigninToken"),
                "Issuer", "https://beam.psdops.com/",
                "Destination", consoleUrl,
                "Action", "login");

        if (urlOnly) {
            out.write(loginUrl + "\n");
            out.flush();
        } else {
            Desktop.getDesktop().browse(new URI(loginUrl));
        }
    }
}
