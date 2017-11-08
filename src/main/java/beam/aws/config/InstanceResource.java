package beam.aws.config;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import org.fusesource.jansi.AnsiRenderWriter;

public class InstanceResource extends TaggableEC2Resource<Instance> {

    private Boolean ebsOptimized;
    private Boolean elasticIp;
    private String elasticIpAllocationId;
    private BeamReference iamInstanceProfile;
    private String imageId;
    private String instanceId;
    private String instanceInitiatedShutdownBehavior;
    private String instanceType;
    private String kernelId;
    private String keyName;
    private Boolean monitoring;
    private List<InstanceNetworkInterfaceSpecification> networkInterfaces;
    private String ramdiskId;
    private String privateIpAddress;
    private String publicIpAddress;
    private String publicDnsName;
    private Set<BeamReference> securityGroups;
    private Boolean sourceDestCheck;
    private String state;
    private BeamReference subnet;
    private String userData;
    private List<EbsResource> volumes;
    private Integer beamLaunchIndex;

    @ResourceDiffProperty
    public Boolean getEbsOptimized() {
        return ebsOptimized;
    }

    public void setEbsOptimized(Boolean ebsOptimized) {
        this.ebsOptimized = ebsOptimized;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getElasticIp() {
        return elasticIp;
    }

    public void setElasticIp(Boolean elasticIp) {
        this.elasticIp = elasticIp;
    }

    public String getElasticIpAllocationId() {
        return elasticIpAllocationId;
    }

    public void setElasticIpAllocationId(String elasticIpAllocationId) {
        this.elasticIpAllocationId = elasticIpAllocationId;
    }

    @ResourceDiffProperty
    public BeamReference getIamInstanceProfile() {
        return iamInstanceProfile;
    }

    public void setIamInstanceProfile(BeamReference iamInstanceProfile) {
        this.iamInstanceProfile = iamInstanceProfile;
    }

    @ResourceDiffProperty
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getInstanceInitiatedShutdownBehavior() {
        return instanceInitiatedShutdownBehavior;
    }

    public void setInstanceInitiatedShutdownBehavior(String instanceInitiatedShutdownBehavior) {
        this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
    }

    @ResourceDiffProperty(updatable = true)
    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @ResourceDiffProperty
    public String getKernelId() {
        return kernelId;
    }

    public void setKernelId(String kernelId) {
        this.kernelId = kernelId;
    }

    @ResourceDiffProperty
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Boolean getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Boolean monitoring) {
        this.monitoring = monitoring;
    }

    /**
     * @return Never {@code null}.
     */
    public List<InstanceNetworkInterfaceSpecification> getNetworkInterfaces() {
        if (networkInterfaces == null) {
            networkInterfaces = new ArrayList<>();
        }
        return networkInterfaces;
    }

    public void setNetworkInterfaces(List<InstanceNetworkInterfaceSpecification> networkInterfaces) {
        this.networkInterfaces = networkInterfaces;
    }

    @ResourceDiffProperty
    public String getRamdiskId() {
        return ramdiskId;
    }

    public void setRamdiskId(String ramdiskId) {
        this.ramdiskId = ramdiskId;
    }

    @ResourceDiffProperty
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPublicDnsName() {
        return publicDnsName;
    }

    public void setPublicDnsName(String publicDnsName) {
        this.publicDnsName = publicDnsName;
    }

    /**
     * @return Never {@code null}.
     */
    @ResourceDiffProperty(updatable = true)
    public Set<BeamReference> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new NullSet<>();
        }
        return securityGroups;
    }

    public void setSecurityGroups(Set<BeamReference> securityGroups) {
        this.securityGroups = securityGroups;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getSourceDestCheck() {
        return sourceDestCheck;
    }

    public void setSourceDestCheck(Boolean sourceDestCheck) {
        this.sourceDestCheck = sourceDestCheck;
    }

    @ResourceDiffProperty(updatable = true)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ResourceDiffProperty
    public BeamReference getSubnet() {
        return newParentReference(SubnetResource.class, subnet);
    }

    public void setSubnet(BeamReference subnet) {
        this.subnet = subnet;
    }

    @ResourceDiffProperty(updatable = true)
    public String getUserDataBlob() {
        return new String(BaseEncoding.base64().decode(userData));
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public List<EbsResource> getVolumes() {
        if (volumes == null) {
            volumes = new ArrayList<>();
        }

        Collections.sort(volumes, new Comparator<EbsResource>() {
            @Override
            public int compare(EbsResource o1, EbsResource o2) {
                return o1.getDeviceName().compareTo(o2.getDeviceName());
            }
        });

        return volumes;
    }

    public void setVolumes(List<EbsResource> volumes) {
        this.volumes = volumes;
    }

    public Integer getBeamLaunchIndex() {
        return beamLaunchIndex;
    }

    public void setBeamLaunchIndex(Integer beamLaunchIndex) {
        this.beamLaunchIndex = beamLaunchIndex;
    }

    public boolean isStopped() {
        return InstanceStateName.Stopped.toString().equals(getState());
    }

    public boolean isRunning() {
        return InstanceStateName.Running.toString().equals(getState());
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String awsId() {
        return instanceId;
    }

    @Override
    public Set<String> taggableAwsIds() {
        return Sets.newHashSet(instanceId);
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getTags().get("beam.layer") + " " + getBeamLaunchIndex());
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getVolumes());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Instance> current) throws Exception {
        InstanceResource currentInstance = (InstanceResource) current;

        // Remove AMI EBS volumes the current and pending images are different.
        if (!((InstanceResource) current).getImageId().equals(getImageId())) {
            Iterator<EbsResource> iter = getVolumes().iterator();
            while (iter.hasNext()) {
                EbsResource resource = iter.next();

                if (resource.isCopiedFromImage()) {
                    iter.remove();
                }
            }
        }

        update.update(currentInstance.getVolumes(), getVolumes());
    }

    @Override
    public void doInit(AWSCloud cloud, BeamResourceFilter filter, Instance instance) {
        setEbsOptimized(instance.getEbsOptimized());
        setImageId(instance.getImageId());
        setInstanceType(instance.getInstanceType());
        setKernelId(instance.getKernelId());
        setKeyName(instance.getKeyName());
        setPrivateIpAddress(instance.getPrivateIpAddress());
        setRamdiskId(instance.getRamdiskId());
        setSourceDestCheck(instance.getSourceDestCheck());
        setState(instance.getState().getName());
        setSubnet(newReference(SubnetResource.class, instance.getSubnetId()));
        setInstanceId(instance.getInstanceId());

        // Associated with an elastic IP?
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DescribeAddressesRequest daRequest = new DescribeAddressesRequest();

        daRequest.setFilters(Arrays.asList(
                new Filter("instance-id").withValues(instance.getInstanceId())));

        for (Address address : client.describeAddresses(daRequest).getAddresses()) {
            setElasticIp(Boolean.TRUE);
            setPublicIpAddress(address.getPublicIp());
            setPublicDnsName(instance.getPublicDnsName());
            break;
        }

        if (getElasticIp() == null) {
            setElasticIp(false);
        }

        if (getPublicIpAddress() == null) {
            setPublicIpAddress(instance.getPublicIpAddress());
            setPublicDnsName(instance.getPublicDnsName());
        }

        // IAM instance profile.
        IamInstanceProfile profile = instance.getIamInstanceProfile();
        if (profile != null) {
            String arn = profile.getArn();
            int lastSlashAt = arn.lastIndexOf('/');

            if (lastSlashAt > -1) {
                setIamInstanceProfile(newReference(InstanceProfileResource.class, arn.substring(lastSlashAt + 1)));
            }
        }

        // Security groups.
        for (GroupIdentifier sg : instance.getSecurityGroups()) {
            getSecurityGroups().add(newReference(SecurityGroupResource.class, sg.getGroupId()));
        }

        DescribeInstanceAttributeRequest dIARequest = new DescribeInstanceAttributeRequest();
        dIARequest.setInstanceId(instance.getInstanceId());
        dIARequest.setAttribute("userData");
        String userData = client.describeInstanceAttribute(dIARequest).getInstanceAttribute().getUserData();
        setUserData(userData);

        // EBS volumes.
        DescribeVolumesRequest dvRequest = new DescribeVolumesRequest();
        dvRequest.setFilters(Arrays.asList(
                new Filter("attachment.instance-id").withValues(Arrays.asList(getInstanceId()))));

        List<CompletableFuture> volumeFutures = new ArrayList<>();
        List<EbsResource> volumeList = new ArrayList<>();
        for (Volume volume : client.describeVolumes(dvRequest).getVolumes()) {
            EbsResource ebsResource = new EbsResource();
            ebsResource.setInstance(newReference(InstanceResource.class, getInstanceId()));
            ebsResource.initAsync(volumeFutures, cloud, filter, volume);
            volumeList.add(ebsResource);
        }

        pollFutures(volumeFutures);
        for (EbsResource volume : volumeList) {
            getVolumes().add(volume);
        }
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        runInstances(client, 1);
        modifyAttributes(client);

        if (Boolean.TRUE.equals(getElasticIp())) {
            allocateElasticIp(client);
        }
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<Instance> current, Set<String> changedProperties) {
        PrintWriter out = new AnsiRenderWriter(System.out, true);

        InstanceResource currentResource = (InstanceResource) current;
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        if (currentResource.isStopped()) {
            ModifyInstanceAttributeRequest miaRequest = new ModifyInstanceAttributeRequest();
            boolean needModifyAttribute = false;
            if (changedProperties.contains("instanceType")) {
                miaRequest.setInstanceType(getInstanceType());
                needModifyAttribute = true;
            } else if (changedProperties.contains("userDataBlob")) {
                miaRequest.setUserData(getUserData());
                needModifyAttribute = true;
            }

            if (needModifyAttribute) {
                executeModifyAttributes(client, miaRequest);
            }
        } else {
            if (changedProperties.contains("instanceType")) {
                out.println("");
                out.print(String.format("           @|bold,blue   Skipping since instance must be stopped to change type from %s to %s|@",
                        currentResource.getInstanceType(), getInstanceType()));
                out.flush();
            } else if (changedProperties.contains("userDataBlob")) {
                out.println("");
                out.print(String.format("           @|bold,blue   Skipping since instance must be stopped to change userData from %s to %s|@",
                        currentResource.getUserDataBlob(), getUserDataBlob()));
                out.flush();
            }
        }

        if (!InstanceStateName.Running.toString().equals(getState())) {
            runInstances(client, 1);
        }

        modifyAttributes(client);

        // Security groups change.
        if (changedProperties.contains("securityGroups")) {
            ModifyInstanceAttributeRequest miaRequest = new ModifyInstanceAttributeRequest();

            miaRequest.setGroups(awsIdSet(getSecurityGroups()));
            executeModifyAttributes(client, miaRequest);
        }

        // State change?
        if (changedProperties.contains("state")) {
            if (currentResource.isStopped()) {
                // Start non-running instances.
                StartInstancesRequest siRequest = new StartInstancesRequest();
                siRequest.getInstanceIds().add(currentResource.getInstanceId());

                client.startInstances(siRequest);
                waitForRunningInstances(client);
            } else if (currentResource.isRunning()) {
                // Stop non-stopped instances.
                StopInstancesRequest siRequest = new StopInstancesRequest();
                siRequest.getInstanceIds().add(currentResource.getInstanceId());

                client.stopInstances(siRequest);
            }
        }

        // Elastic IP.
        if (changedProperties.contains("elasticIp")) {
            if (getElasticIp()) {
                allocateElasticIp(client);

            } else {
                releaseElasticIp(client);
            }
        }
    }

    private void modifyAttributes(AmazonEC2Client client) {
        Boolean sourceDestCheck = getSourceDestCheck();

        if (sourceDestCheck != null) {
            ModifyInstanceAttributeRequest miaRequest = new ModifyInstanceAttributeRequest();

            miaRequest.setSourceDestCheck(sourceDestCheck);
            executeModifyAttributes(client, miaRequest);
        }
    }

    private void executeModifyAttributes(AmazonEC2Client client, ModifyInstanceAttributeRequest miaRequest) {
        miaRequest.setInstanceId(getInstanceId());
        client.modifyInstanceAttribute(miaRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        releaseElasticIp(client);
        terminateInstance(client);
    }

    private void runInstances(AmazonEC2Client client, int count) {
        RunInstancesRequest riRequest = new RunInstancesRequest();

        riRequest.setEbsOptimized(getEbsOptimized());
        riRequest.setImageId(getImageId());
        riRequest.setInstanceInitiatedShutdownBehavior(getInstanceInitiatedShutdownBehavior());
        riRequest.setInstanceType(getInstanceType());
        riRequest.setKernelId(getKernelId());
        riRequest.setKeyName(getKeyName());
        riRequest.setMaxCount(count);
        riRequest.setMinCount(count);
        riRequest.setMonitoring(getMonitoring());
        riRequest.setPrivateIpAddress(getPrivateIpAddress());
        riRequest.setRamdiskId(getRamdiskId());
        riRequest.setUserData(getUserData());

        // Security groups and subnet for either the implicit network
        // interface or explicitly configured ones.
        List<InstanceNetworkInterfaceSpecification> nis = getNetworkInterfaces();
        Set<String> securityGroupIds = awsIdSet(getSecurityGroups());
        String subnetId = getSubnet().awsId();

        if (nis.isEmpty()) {
            riRequest.setSecurityGroupIds(securityGroupIds);
            riRequest.setSubnetId(subnetId);

        } else {
            riRequest.setNetworkInterfaces(nis);

            for (InstanceNetworkInterfaceSpecification ni : nis) {
                ni.setGroups(securityGroupIds);
                ni.setSubnetId(subnetId);
            }
        }

        // Instance profile.
        BeamReference profile = getIamInstanceProfile();

        if (profile != null) {
            String profileName = profile.awsId();

            if (profileName != null) {
                IamInstanceProfileSpecification profileSpec = new IamInstanceProfileSpecification();

                profileSpec.setName(profileName);
                riRequest.setIamInstanceProfile(profileSpec);
            }
        }

        // Run the request and sync the instance IDs.
        RunInstancesResult result = (RunInstancesResult) executeService(() -> client.runInstances(riRequest));

        for (Instance i : result.getReservation().getInstances()) {
            instanceId = i.getInstanceId();
        }

        waitForRunningInstances(client);
    }

    private void waitForRunningInstances(AmazonEC2Client client) {
        // Wait for the instance to be not pending.
        boolean running = false;
        while (!running) {
            DescribeInstancesRequest diRequest = new DescribeInstancesRequest();

            diRequest.setInstanceIds(Lists.newArrayList(instanceId));

            try {
                for (Reservation r : client.
                        describeInstances(diRequest).
                        getReservations()) {

                    for (Instance i : r.getInstances()) {
                        if ("running".equals(i.getState().getName())) {
                            setPublicDnsName(i.getPublicDnsName());
                            setPublicIpAddress(i.getPublicIpAddress());
                            setPrivateIpAddress(i.getPrivateIpAddress());
                            running = true;
                        }
                    }
                }
            } catch (AmazonServiceException error) {
                // Amazon sometimes doesn't make the instances available
                // immediately for API requests.
                if (!"InvalidInstanceID.NotFound".equals(error.getErrorCode())) {
                    throw error;
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                return;
            }
        }
    }

    private void terminateInstance(AmazonEC2Client client) {
        TerminateInstancesRequest tiRequest = new TerminateInstancesRequest();

        tiRequest.setInstanceIds(Lists.newArrayList(getInstanceId()));
        client.terminateInstances(tiRequest);

        // Wait for the instance to be really terminated.
        boolean terminated = false;
        while (!terminated) {
            DescribeInstancesRequest diRequest = new DescribeInstancesRequest();

            diRequest.setInstanceIds(Lists.newArrayList(getInstanceId()));

            for (Reservation r : client.
                    describeInstances(diRequest).
                    getReservations()) {

                for (Instance i : r.getInstances()) {
                    if ("terminated".equals(i.getState().getName())) {
                        terminated = true;
                    }
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                return;
            }
        }
    }

    private void allocateElasticIp(AmazonEC2Client client) {
        AllocateAddressRequest alaRequest = new AllocateAddressRequest();

        alaRequest.setDomain(DomainType.Vpc);

        if (getElasticIpAllocationId() == null) {
            AllocateAddressResult alaResult = client.allocateAddress(alaRequest);
            setPublicIpAddress(alaResult.getPublicIp());
            setElasticIpAllocationId(alaResult.getAllocationId());
        } else {
            setPublicIpAddress(findElasticIpAddress(client, getElasticIpAllocationId()));
        }

        AssociateAddressRequest asaRequest = new AssociateAddressRequest();

        asaRequest.setAllocationId(getElasticIpAllocationId());
        asaRequest.setInstanceId(instanceId);
        executeService(() -> client.associateAddress(asaRequest));
    }

    private void releaseElasticIp(AmazonEC2Client client) {
        DescribeAddressesRequest deaRequest = new DescribeAddressesRequest();

        deaRequest.setFilters(Arrays.asList(
                new Filter("instance-id").withValues(instanceId)));

        for (Address address : client.
                describeAddresses(deaRequest).
                getAddresses()) {

            DisassociateAddressRequest diaRequest = new DisassociateAddressRequest();

            diaRequest.setAssociationId(address.getAssociationId());
            client.disassociateAddress(diaRequest);

            ReleaseAddressRequest raRequest = new ReleaseAddressRequest();

            raRequest.setAllocationId(address.getAllocationId());
            client.releaseAddress(raRequest);
        }
    }

    private String findElasticIpAddress(AmazonEC2Client client, String allocationId) {
        DescribeAddressesRequest daRequest = new DescribeAddressesRequest();
        daRequest.setAllocationIds(Arrays.asList(allocationId));

        DescribeAddressesResult daResult = client.describeAddresses(daRequest);

        for (Address address : daResult.getAddresses()) {
            return address.getPublicIp();
        }

        return null;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getTags().get("beam.project"));
        sb.append(" ");
        sb.append(getTags().get("beam.env"));
        sb.append(" serial-");
        sb.append(getTags().get("beam.serial"));
        sb.append(" ");
        sb.append(getTags().get("beam.layer"));
        sb.append(" layer instance");

        if (instanceId != null) {
            sb.append(" [" + instanceId + "]");
        }

        return sb.toString();
    }
}
