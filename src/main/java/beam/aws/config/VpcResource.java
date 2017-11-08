package beam.aws.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcAttributeRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.ModifyVpcAttributeRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcAttributeName;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointsResult;
import com.amazonaws.services.ec2.model.VpcEndpoint;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;

public class VpcResource extends TaggableEC2Resource<Vpc> {

    private String cidrBlock;
    private String vpcId;

    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;

    private Set<AutoScalingGroupResource> autoScalingGroups;
    private Set<InstanceProfileResource> instanceProfiles;
    private InternetGatewayResource internetGateway;
    private Set<KeyPairResource> keyPairs;
    private Set<LaunchConfigurationResource> launchConfigurations;
    private Set<LoadBalancerResource> loadBalancers;
    private Set<RoleResource> roles;
    private Set<SecurityGroupResource> securityGroups;
    private Set<SubnetResource> subnets;
    private Set<VpcEndpointResource> vpcEndpoints;

    @ResourceDiffProperty
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsHostnames() {
        return enableDnsHostnames;
    }

    public void setEnableDnsHostnames(Boolean enableDnsHostnames) {
        this.enableDnsHostnames = enableDnsHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsSupport() {
        return enableDnsSupport;
    }

    public void setEnableDnsSupport(Boolean enableDnsSupport) {
        this.enableDnsSupport = enableDnsSupport;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<AutoScalingGroupResource> getAutoScalingGroups() {
        if (autoScalingGroups == null) {
            autoScalingGroups = new LinkedHashSet<>();
        }
        return autoScalingGroups;
    }

    public void setAutoScalingGroups(Set<AutoScalingGroupResource> autoScalingGroups) {
        this.autoScalingGroups = autoScalingGroups;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<InstanceProfileResource> getInstanceProfiles() {
        if (instanceProfiles == null) {
            instanceProfiles = new HashSet<>();
        }
        return instanceProfiles;
    }

    public void setInstanceProfiles(Set<InstanceProfileResource> instanceProfiles) {
        this.instanceProfiles = instanceProfiles;
    }

    public InternetGatewayResource getInternetGateway() {
        return internetGateway;
    }

    public void setInternetGateway(InternetGatewayResource internetGateway) {
        this.internetGateway = internetGateway;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<KeyPairResource> getKeyPairs() {
        if (keyPairs == null) {
            keyPairs = new HashSet<>();
        }
        return keyPairs;
    }

    public void setKeyPairs(Set<KeyPairResource> keyPairs) {
        this.keyPairs = keyPairs;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<LaunchConfigurationResource> getLaunchConfigurations() {
        if (launchConfigurations == null) {
            launchConfigurations = new HashSet<>();
        }
        return launchConfigurations;
    }

    public void setLaunchConfigurations(
            Set<LaunchConfigurationResource> launchConfigurations) {
        this.launchConfigurations = launchConfigurations;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<LoadBalancerResource> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new HashSet<>();
        }
        return loadBalancers;
    }

    public void setLoadBalancers(Set<LoadBalancerResource> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<RoleResource> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<RoleResource> roles) {
        this.roles = roles;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<SecurityGroupResource> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new HashSet<>();
        }
        return securityGroups;
    }

    public void setSecurityGroups(Set<SecurityGroupResource> securityGroups) {
        this.securityGroups = securityGroups;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<SubnetResource> getSubnets() {
        if (subnets == null) {
            subnets = new HashSet<>();
        }
        return subnets;
    }

    public void setSubnets(Set<SubnetResource> subnets) {
        this.subnets = subnets;
    }

    public Set<VpcEndpointResource> getVpcEndpoints() {
        if (vpcEndpoints == null) {
            vpcEndpoints = new HashSet<>();
        }
        return vpcEndpoints;
    }

    public void setVpcEndpoints(Set<VpcEndpointResource> vpcEndpoints) {
        this.vpcEndpoints = vpcEndpoints;
    }

    @Override
    public String awsId() {
        return getVpcId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getCidrBlock(), getVpcId());
    }

    @Override
    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, Vpc vpc) {
        AmazonEC2Client ec2Client = createClient(AmazonEC2Client.class, cloud.getProvider());
        String vpcId = vpc.getVpcId();

        setCidrBlock(vpc.getCidrBlock());
        setVpcId(vpcId);

        // VPC attributes.
        DescribeVpcAttributeRequest dvaRequest = new DescribeVpcAttributeRequest();

        dvaRequest.setVpcId(vpcId);
        dvaRequest.setAttribute(VpcAttributeName.EnableDnsHostnames);
        setEnableDnsHostnames(ec2Client.
                describeVpcAttribute(dvaRequest).
                isEnableDnsHostnames());

        dvaRequest.setAttribute(VpcAttributeName.EnableDnsSupport);
        setEnableDnsSupport(ec2Client.
                describeVpcAttribute(dvaRequest).
                isEnableDnsSupport());

        // Internet gateway.
        DescribeInternetGatewaysRequest digRequest = new DescribeInternetGatewaysRequest();

        digRequest.setFilters(Arrays.asList(
                new Filter("attachment.vpc-id").withValues(vpcId)));

        for (InternetGateway ig : ec2Client.
                describeInternetGateways(digRequest).
                getInternetGateways()) {

            if (isInclude(filter, ig)) {
                InternetGatewayResource igResource = new InternetGatewayResource();
                igResource.setRegion(getRegion());

                igResource.init(cloud, filter, ig);
                setInternetGateway(igResource);
                break;
            }
        }

        // Instance profiles.
        AmazonIdentityManagementClient imClient = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());

        List<CompletableFuture> ipFutures = new ArrayList<>();
        ListInstanceProfilesRequest lipRequest = new ListInstanceProfilesRequest();
        ListInstanceProfilesResult lipResult;

        do {
            lipResult = imClient.listInstanceProfiles(lipRequest);
            for (InstanceProfile ip : lipResult.getInstanceProfiles()) {
                if (isInclude(filter, ip)) {
                    InstanceProfileResource ipResource = new InstanceProfileResource();
                    ipResource.setRegion(getRegion());

                    ipResource.initAsync(ipFutures, cloud, filter, ip);
                    getInstanceProfiles().add(ipResource);
                }
            }

            lipRequest.setMarker(lipResult.getMarker());
        } while (lipResult.isTruncated());

        pollFutures(ipFutures);

        // Roles.
        List<CompletableFuture> roleFutures = new ArrayList<>();
        ListRolesRequest lrRequest = new ListRolesRequest();
        ListRolesResult lrResult;

        do {
            lrResult = imClient.listRoles(lrRequest);
            for (Role role : lrResult.getRoles()) {
                if (isInclude(filter, role)) {
                    RoleResource roleResource = new RoleResource();
                    roleResource.setRegion(getRegion());

                    roleResource.initAsync(roleFutures, cloud, filter, role);
                    getRoles().add(roleResource);
                }
            }

            lrRequest.setMarker(lrResult.getMarker());
        } while (lrResult.isTruncated());

        pollFutures(roleFutures);

        // Security groups.
        DescribeSecurityGroupsRequest sgsRequest = new DescribeSecurityGroupsRequest();
        List<Filter> vpcIdFilter = Arrays.asList(new Filter("vpc-id").withValues(vpcId));

        sgsRequest.setFilters(vpcIdFilter);

        List<CompletableFuture> sgFutures = new ArrayList<>();
        for (SecurityGroup sg : ec2Client.
                describeSecurityGroups(sgsRequest).
                getSecurityGroups()) {

            if (!"default".equals(sg.getGroupName()) &&
                    isInclude(filter, sg)) {

                SecurityGroupResource sgResource = new SecurityGroupResource();
                sgResource.setRegion(getRegion());

                sgResource.initAsync(sgFutures, cloud, filter, sg);
                getSecurityGroups().add(sgResource);
            }
        }

        pollFutures(sgFutures);

        // Subnets.
        DescribeSubnetsRequest subnetsRequest = new DescribeSubnetsRequest();

        subnetsRequest.setFilters(vpcIdFilter);

        List<CompletableFuture> subnetFutures = new ArrayList<>();
        for (Subnet subnet : ec2Client.
                describeSubnets(subnetsRequest).
                getSubnets()) {

            if (isInclude(filter, subnet)) {
                SubnetResource subnetResource = new SubnetResource();
                subnetResource.setRegion(getRegion());

                subnetResource.initAsync(subnetFutures, cloud, filter, subnet);
                getSubnets().add(subnetResource);
            }
        }

        pollFutures(subnetFutures);

        // Load balancers.
        AmazonElasticLoadBalancingClient elbClient = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());

        List<CompletableFuture> elbFutures = new ArrayList<>();
        for (LoadBalancerDescription lb : elbClient.describeLoadBalancers().getLoadBalancerDescriptions()) {
            if (vpcId.equals(lb.getVPCId()) &&
                    !lb.getLoadBalancerName().endsWith("-v") &&
                    isInclude(filter, lb)) {

                LoadBalancerResource lbResource = new LoadBalancerResource();
                lbResource.setRegion(getRegion());
                lbResource.initAsync(elbFutures, cloud, filter, lb);
                getLoadBalancers().add(lbResource);
            }
        }

        pollFutures(elbFutures);

        // Auto scaling groups that span subnets in the VPC.
        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        Set<String> subnetIds = new HashSet<>();

        for (SubnetResource subnet : getSubnets()) {
            String subnetId = subnet.getSubnetId();

            if (subnetId != null) {
                subnetIds.add(subnetId);
            }
        }

        if (!subnetIds.isEmpty()) {
            Set<String> asgNames = new HashSet<>();

            List<CompletableFuture> asFutures = new ArrayList<>();
            for (AutoScalingGroup asg : asClient.
                    describeAutoScalingGroups().
                    getAutoScalingGroups()) {

                String zoneId = asg.getVPCZoneIdentifier();

                if (zoneId != null &&
                        !Collections.disjoint(subnetIds, Arrays.asList(zoneId.split("\\s*,\\s*"))) &&
                        isInclude(filter, asg)) {

                    asgNames.add(asg.getAutoScalingGroupName());

                    AutoScalingGroupResource asgResource = new AutoScalingGroupResource();
                    asgResource.setRegion(getRegion());

                    asgResource.initAsync(asFutures, cloud, filter, asg);
                    getAutoScalingGroups().add(asgResource);
                }
            }

            pollFutures(asFutures);

            if (!asgNames.isEmpty()) {

                List<CompletableFuture> lcFutures = new ArrayList<>();
                for (LaunchConfiguration lc : asClient.
                        describeLaunchConfigurations().
                        getLaunchConfigurations()) {

                    for (String asgName : asgNames) {
                        if (lc.getLaunchConfigurationName().startsWith(asgName) &&
                                isInclude(filter, lc)) {

                            LaunchConfigurationResource lcResource = new LaunchConfigurationResource();
                            lcResource.setRegion(getRegion());

                            lcResource.initAsync(lcFutures, cloud, filter, lc);
                            getLaunchConfigurations().add(lcResource);
                            break;
                        }
                    }
                }

                pollFutures(lcFutures);
            }
        }

        // Key pairs.
        DescribeKeyPairsRequest dkpRequest = new DescribeKeyPairsRequest();

        List<CompletableFuture> keyPairFutures = new ArrayList<>();
        for (KeyPairInfo kp : ec2Client.
                describeKeyPairs(dkpRequest).
                getKeyPairs()) {

            String keypairName = String.format("%s-%s", cloud.getProject(), getRegion());
            if (kp.getKeyName().equals(keypairName)) {
                KeyPairResource kpResource = new KeyPairResource();
                kpResource.setRegion(getRegion());

                kpResource.initAsync(keyPairFutures, cloud, filter, kp);
                getKeyPairs().add(kpResource);
            }
        }

        pollFutures(keyPairFutures);

        // VpcEndpoints
        DescribeVpcEndpointsRequest dveRequest = new DescribeVpcEndpointsRequest();
        dveRequest.withFilters(Arrays.asList(
                new Filter("vpc-id").withValues(getVpcId())));

        DescribeVpcEndpointsResult dveResult = ec2Client.describeVpcEndpoints(dveRequest);

        for (VpcEndpoint vpcEndpoint: dveResult.getVpcEndpoints()) {
            VpcEndpointResource vpcEndpointResource = new VpcEndpointResource();
            vpcEndpointResource.init(cloud, filter, vpcEndpoint);
            getVpcEndpoints().add(vpcEndpointResource);
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.createOne(getInternetGateway());
        create.create(getKeyPairs());
        create.create(getRoles());
        create.create(getInstanceProfiles());
        create.create(getSecurityGroups());
        create.create(getSubnets());
        create.create(getLoadBalancers());
        create.create(getLaunchConfigurations());
        create.create(getAutoScalingGroups());
        create.create(getVpcEndpoints());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Vpc> current) throws Exception {
        VpcResource currentVpc = (VpcResource) current;

        update.updateOne(currentVpc.getInternetGateway(), getInternetGateway());
        update.update(currentVpc.getKeyPairs(), getKeyPairs());
        update.update(currentVpc.getRoles(), getRoles());
        update.update(currentVpc.getInstanceProfiles(), getInstanceProfiles());
        update.update(currentVpc.getSecurityGroups(), getSecurityGroups());
        update.update(currentVpc.getSubnets(), getSubnets());
        update.update(currentVpc.getLoadBalancers(), getLoadBalancers());
        update.update(currentVpc.getLaunchConfigurations(), getLaunchConfigurations());
        update.update(currentVpc.getAutoScalingGroups(), getAutoScalingGroups());
        update.update(currentVpc.getVpcEndpoints(), getVpcEndpoints());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getAutoScalingGroups());
        delete.delete(getLaunchConfigurations());
        delete.delete(getLoadBalancers());
        delete.delete(getSubnets());
        delete.delete(getSecurityGroups());
        delete.delete(getInstanceProfiles());
        delete.delete(getRoles());
        delete.delete(getKeyPairs());
        delete.deleteOne(getInternetGateway());
        delete.delete(getVpcEndpoints());
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateVpcRequest cvRequest = new CreateVpcRequest();

        cvRequest.setCidrBlock(getCidrBlock());
        setVpcId(client.createVpc(cvRequest).getVpc().getVpcId());

        modifyAttributes(client);
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<Vpc> current, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        modifyAttributes(client);
    }

    private void modifyAttributes(AmazonEC2Client client) {
        String vpcId = getVpcId();
        Boolean dnsHostnames = getEnableDnsHostnames();

        if (dnsHostnames != null) {
            ModifyVpcAttributeRequest mvaRequest = new ModifyVpcAttributeRequest();

            mvaRequest.setVpcId(vpcId);
            mvaRequest.setEnableDnsHostnames(dnsHostnames);
            client.modifyVpcAttribute(mvaRequest);
        }

        Boolean dnsSupport = getEnableDnsSupport();

        if (dnsSupport != null) {
            ModifyVpcAttributeRequest mvaRequest = new ModifyVpcAttributeRequest();

            mvaRequest.setVpcId(vpcId);
            mvaRequest.setEnableDnsSupport(dnsSupport);
            client.modifyVpcAttribute(mvaRequest);
        }
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteVpcRequest dvRequest = new DeleteVpcRequest();

        dvRequest.setVpcId(getVpcId());
        client.deleteVpc(dvRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String vpcId = getVpcId();

        if (vpcId != null) {
            sb.append(vpcId);

        } else {
            sb.append("VPC");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(cidrBlock);
        }

        return sb.toString();
    }
}
