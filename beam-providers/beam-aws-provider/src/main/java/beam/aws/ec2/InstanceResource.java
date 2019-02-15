package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.BeamInstance;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.codec.binary.Base64;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.CapacityReservationSpecification;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceAttributeName;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.MonitoringState;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.ShutdownBehavior;
import software.amazon.awssdk.utils.builder.SdkBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates an Instance with the specified AMI, Subnet and Security group.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::instance instance
 *         ami-name: "amzn-ami-hvm-2018.03.0.20181129-x86_64-gp2"
 *         shutdown-behavior: "stop"
 *         instance-type: "t2.micro"
 *         key-name: "instance-static"
 *         subnet-id: $(aws::subnet subnet-instance-example | subnet-id)
 *         security-group-ids: [
 *             $(aws::security-group security-group-instance-example-1 | group-id),
 *             $(aws::security-group security-group-instance-example-2 | group-id)
 *         ]
 *         disable-api-termination: false
 *         enable-ena-support: true
 *         ebs-optimized: false
 *         source-dest-check: true
 *
 *         tags: {
 *             Name: "instance-example"
 *         }
 *     end
 */
@ResourceName("instance")
public class InstanceResource extends Ec2TaggableResource<Instance> implements BeamInstance {

    private String amiId;
    private String amiName;
    private Integer coreCount;
    private Integer threadPerCore;
    private Boolean ebsOptimized;
    private Boolean configureHibernateOption;
    private String shutdownBehavior;
    private String instanceType;
    private String keyName;
    private Boolean enableMonitoring;
    private List<String> securityGroupIds;
    private String subnetId;
    private Boolean disableApiTermination;
    private Boolean enableEnaSupport;
    private Boolean sourceDestCheck;
    private String userData;
    private String capacityReservation;

    // -- Readonly

    private String instanceId;
    private String privateIpAddress;
    private String publicIpAddress;
    private String publicDnsName;
    private String instanceState;
    private Date launchDate;

    /**
     * Instance ID of this instance.
     *
     * @output
     */
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * The ID of an AMI that would be used to launch the instance. Required if AMI Name not provided. See Finding an AMI `<https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html/>`_.
     */
    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    /**
     * The Name of an AMI that would be used to launch the instance. Required if AMI Id not provided. See Amazon Machine Images (AMI) `<https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html/>`_.
     */
    public String getAmiName() {
        return amiName;
    }

    public void setAmiName(String amiName) {
        this.amiName = amiName;
    }

    /**
     * Launch instances with defined number of cores. Defaults to 0 which sets its to the instance type defaults. See `Optimizing CPU Options <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-optimize-cpu.html/>`_.
     */
    public Integer getCoreCount() {
        if (coreCount == null) {
            coreCount = 0;
        }

        return coreCount;
    }

    public void setCoreCount(Integer coreCount) {
        this.coreCount = coreCount;
    }

    /**
     * Launch instances with defined number of threads per cores. Defaults to 0 which sets its to the instance type defaults. See `Optimizing CPU Options <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-optimize-cpu.html/>`_.
     */
    public Integer getThreadPerCore() {
        if (threadPerCore == null) {
            threadPerCore = 0;
        }

        return threadPerCore;
    }

    public void setThreadPerCore(Integer threadPerCore) {
        this.threadPerCore = threadPerCore;
    }

    /**
     * Enable EBS optimization for an instance. Defaults to false. See `Amazon EBS–Optimized Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSOptimized.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEbsOptimized() {
        if (ebsOptimized == null) {
            ebsOptimized = false;
        }

        return ebsOptimized;
    }

    public void setEbsOptimized(Boolean ebsOptimized) {
        this.ebsOptimized = ebsOptimized;
    }

    /**
     * Enable Hibernate options for an instance. Defaults to false. See `Hibernate your Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Hibernate.html/>`_.
     */
    public Boolean getConfigureHibernateOption() {
        if (configureHibernateOption == null) {
            configureHibernateOption = false;
        }

        return configureHibernateOption;
    }

    public void setConfigureHibernateOption(Boolean configureHibernateOption) {
        this.configureHibernateOption = configureHibernateOption;
    }

    /**
     * Change the Shutdown Behavior options for an instance. Defaults to Stop. See `Changing the Instance Initiated Shutdown Behavior <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/terminating-instances.html#Using_ChangingInstanceInitiatedShutdownBehavior/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getShutdownBehavior() {
        return shutdownBehavior != null ? shutdownBehavior.toLowerCase() : ShutdownBehavior.STOP.toString();
    }

    public void setShutdownBehavior(String shutdownBehavior) {
        this.shutdownBehavior = shutdownBehavior;
    }

    /**
     * Launch instance with the type of hardware you desire. See `Instance Types <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html/>`_. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getInstanceType() {
        return instanceType != null ? instanceType.toLowerCase() : instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    /**
     * Launch instance with the key name of an EC2 Key Pair. This is a certificate required to access your instance. See `Amazon EC2 Key Pairs < https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html/>`_. (Required)
     */
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    /**
     * Enable or Disable monitoring for your instance. See `Monitoring Your Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-cloudwatch.html/>`_.
     */
    public Boolean getEnableMonitoring() {
        if (enableMonitoring == null) {
            enableMonitoring = false;
        }
        return enableMonitoring;
    }

    public void setEnableMonitoring(Boolean enableMonitoring) {
        this.enableMonitoring = enableMonitoring;
    }

    /**
     * Launch instance with the security groups specified. See `Amazon EC2 Security Groups for Linux Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-network-security.html/>`_. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getSecurityGroupIds() {
        if (securityGroupIds == null) {
            securityGroupIds = new ArrayList<>();
        }
        return securityGroupIds;
    }

    public void setSecurityGroupIds(List<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    /**
     * Launch instance with the subnet specified. See `Vpcs and Subnets <https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Subnets.html/>`_. (Required)
     */
    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    /**
     * Enable or Disable api termination of an instance. See `Enabling Termination Protection for an Instance <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/terminating-instances.html#Using_ChangingDisableAPITermination/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDisableApiTermination() {
        if (disableApiTermination == null) {
            disableApiTermination = false;
        }

        return disableApiTermination;
    }

    public void setDisableApiTermination(Boolean disableApiTermination) {
        this.disableApiTermination = disableApiTermination;
    }

    /**
     * Enable or Disable ENA support for an instance. Defaults to true and cannot be turned off during creation. See `Enabling Enhanced Networking with the Elastic Network Adapter (ENA) on Linux Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/enhanced-networking-ena.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableEnaSupport() {
        if (enableEnaSupport == null) {
            enableEnaSupport = true;
        }
        return enableEnaSupport;
    }

    public void setEnableEnaSupport(Boolean enableEnaSupport) {
        this.enableEnaSupport = enableEnaSupport;
    }

    /**
     * Enable or Disable Source/Dest Check for an instance. Defaults to true and cannot be turned off during creation. See `Disabling Source/Destination Checks <https://docs.aws.amazon.com/vpc/latest/userguide/VPC_NAT_Instance.html#EIP_Disable_SrcDestCheck/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getSourceDestCheck() {
        if (sourceDestCheck == null) {
            sourceDestCheck = true;
        }
        return sourceDestCheck;
    }

    public void setSourceDestCheck(Boolean sourceDestCheck) {
        this.sourceDestCheck = sourceDestCheck;
    }

    /**
     * Set user data for your instance. See `Instance Metadata and User Data <https://docs.aws.amazon.com/AWSEC2/latest/WindowsGuide/ec2-instance-metadata.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getUserData() {
        if (userData == null) {
            userData = "";
        } else {
            userData = userData.trim();
        }

        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

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

    public String getInstanceState() {
        return instanceState;
    }

    public void setInstanceState(String instanceState) {
        this.instanceState = instanceState;
    }

    public void setInstanceLaunchDate(Date launchDate) {
        this.launchDate = launchDate;
    }

    public Date getInstanceLaunchDate() {
        return launchDate;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getCapacityReservation() {
        if (capacityReservation == null) {
            capacityReservation = "none";
        }

        return capacityReservation;
    }

    public void setCapacityReservation(String capacityReservation) {
        this.capacityReservation = capacityReservation;
    }

    // -- BeamInstance Implementation

    @Override
    public String getState() {
        return getInstanceState();
    }

    @Override
    public String getHostname() {
        return getPublicDnsName();
    }

    @Override
    public String getName() {
        if (getTags().isEmpty()) {
            return resourceIdentifier();
        }

        return getTags().get("Name");
    }

    @Override
    public String getLaunchDate() {
        if (getInstanceLaunchDate() != null) {
            return getInstanceLaunchDate().toString();
        }

        return "";
    }

    @Override
    protected String getId() {
        return getInstanceId();
    }

    @Override
    protected boolean doRefresh() {

        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getInstanceId())) {
            throw new BeamException("instance-id is missing, unable to load instance.");
        }

        try {
            DescribeInstancesResponse response = client.describeInstances(r -> r.instanceIds(getInstanceId()));

            List<Reservation> reservations = response.reservations();
            for (Reservation reservation : reservations) {
                List<Instance> instances = reservation.instances();
                if (instances.isEmpty())  {
                    return false;
                }

                for (Instance instance : instances) {
                    setAmiId(instance.imageId());
                    setCoreCount(instance.cpuOptions().coreCount());
                    setThreadPerCore(instance.cpuOptions().threadsPerCore());
                    setEbsOptimized(instance.ebsOptimized());
                    setConfigureHibernateOption(instance.hibernationOptions().configured());
                    setInstanceType(instance.instanceType().toString());
                    setKeyName(instance.keyName());
                    setEnableMonitoring(instance.monitoring().state().equals(MonitoringState.ENABLED));
                    setSecurityGroupIds(instance.securityGroups().stream().map(GroupIdentifier::groupId).collect(Collectors.toList()));
                    setSubnetId(instance.subnetId());
                    setEnableEnaSupport(instance.enaSupport());
                    setPublicDnsName(instance.publicDnsName());
                    setPublicIpAddress(instance.publicIpAddress());
                    setPrivateIpAddress(instance.privateIpAddress());
                    setInstanceState(instance.state().nameAsString());
                    setInstanceLaunchDate(Date.from(instance.launchTime()));
                    setCapacityReservation(
                        instance.capacityReservationSpecification().capacityReservationTarget() != null
                            ? instance.capacityReservationSpecification().capacityReservationTarget().capacityReservationId()
                            : instance.capacityReservationSpecification().capacityReservationPreferenceAsString()
                    );

                    if (instance.state().name() == InstanceStateName.TERMINATED) {
                        return false;
                    }

                    break;
                }
                break;
            }

            DescribeInstanceAttributeResponse attributeResponse = client.describeInstanceAttribute(
                r -> r.instanceId(getInstanceId()).attribute(InstanceAttributeName.INSTANCE_INITIATED_SHUTDOWN_BEHAVIOR)
            );
            setShutdownBehavior(attributeResponse.instanceInitiatedShutdownBehavior().value());

            attributeResponse = client.describeInstanceAttribute(
                r -> r.instanceId(getInstanceId()).attribute(InstanceAttributeName.DISABLE_API_TERMINATION)
            );
            setDisableApiTermination(attributeResponse.disableApiTermination().equals(AttributeBooleanValue.builder().value(true).build()));

            attributeResponse = client.describeInstanceAttribute(
                r -> r.instanceId(getInstanceId()).attribute(InstanceAttributeName.SOURCE_DEST_CHECK)
            );
            setSourceDestCheck(attributeResponse.sourceDestCheck().equals(AttributeBooleanValue.builder().value(true).build()));

            attributeResponse = client.describeInstanceAttribute(
                r -> r.instanceId(getInstanceId()).attribute(InstanceAttributeName.USER_DATA)
            );
            setUserData(attributeResponse.userData().value() == null
                ? "" : new String(Base64.decodeBase64(attributeResponse.userData().value())).trim());

        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        validate(client, true);

        try {
            RunInstancesResponse response = client.runInstances(
                r -> r.imageId(getAmiId())
                    .ebsOptimized(getEbsOptimized())
                    .hibernationOptions(o -> o.configured(getConfigureHibernateOption()))
                    .instanceInitiatedShutdownBehavior(getShutdownBehavior())
                    .cpuOptions(getCoreCount() > 0 ? o -> o.threadsPerCore(getThreadPerCore()).coreCount(getCoreCount()).build() : SdkBuilder::build)
                    .instanceType(getInstanceType())
                    .keyName(getKeyName())
                    .maxCount(1)
                    .minCount(1)
                    .monitoring(o -> o.enabled(getEnableMonitoring()))
                    .securityGroupIds(getSecurityGroupIds())
                    .subnetId(getSubnetId())
                    .disableApiTermination(getDisableApiTermination())
                    .userData(new String(Base64.encodeBase64(getUserData().trim().getBytes())))
                    .capacityReservationSpecification(getCapacityReservationSpecification())
            );

            for (Instance instance : response.instances()) {
                setInstanceId(instance.instanceId());
                setPublicDnsName(instance.publicDnsName());
                setPublicIpAddress(instance.publicIpAddress());
                setPrivateIpAddress(instance.privateIpAddress());
                setInstanceState(instance.state().nameAsString());
                setInstanceLaunchDate(Date.from(instance.launchTime()));
            }

            waitForRunningInstances(client);

        } catch (Ec2Exception ex) {
            throw ex;
        }
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        validate(client, false);

        if (changedProperties.contains("shutdown-behavior")) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .instanceInitiatedShutdownBehavior(o -> o.value(getShutdownBehavior()))
            );
        }

        if (changedProperties.contains("disable-api-termination")) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .disableApiTermination(o -> o.value(getDisableApiTermination()))
            );
        }

        if (changedProperties.contains("source-dest-check")) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .sourceDestCheck(o -> o.value(getSourceDestCheck()))
            );
        }

        if (changedProperties.contains("security-group-ids")) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .groups(getSecurityGroupIds())
            );
        }

        boolean instanceStopped = isInstanceStopped(client);

        if (changedProperties.contains("instance-type")
            && validateInstanceStop(instanceStopped, "instance-type", getInstanceType())) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .instanceType(o -> o.value(getInstanceType()))
            );
        }

        if (changedProperties.contains("enable-ena-support")
            && validateInstanceStop(instanceStopped, "enable-ena-support", getEnableEnaSupport().toString())) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .enaSupport(o -> o.value(getEnableEnaSupport()))
            );
        }

        if (changedProperties.contains("ebs-optimized")
            && validateInstanceStop(instanceStopped, "ebs-optimized", getEbsOptimized().toString())) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .ebsOptimized(o -> o.value(getEbsOptimized()))
            );
        }

        if (changedProperties.contains("user-data")
            && validateInstanceStop(instanceStopped, "user-data", getUserData())) {
            client.modifyInstanceAttribute(
                r -> r.instanceId(getInstanceId())
                    .userData(o -> o.value(SdkBytes.fromByteArray(getUserData().getBytes())))
            );
        }

        if (changedProperties.contains("capacity-reservation")
            && validateInstanceStop(instanceStopped, "capacity-reservation", getCapacityReservation())) {
            client.modifyInstanceCapacityReservationAttributes(
                r -> r.instanceId(getInstanceId())
                    .capacityReservationSpecification(getCapacityReservationSpecification())
            );
        }
    }

    @Override
    public void delete() {
        if (getDisableApiTermination()) {
            throw new BeamException("The instance (" + getInstanceId() + ") cannot be terminated when 'disableApiTermination' is set to True.");
        }

        Ec2Client client = createClient(Ec2Client.class);

        client.terminateInstances(r -> r.instanceIds(Collections.singletonList(getInstanceId())));

        // Wait for the instance to be really terminated.
        boolean terminated = false;
        while (!terminated) {
            for (Reservation reservation : client.describeInstances(r -> r.instanceIds(getInstanceId())).reservations()) {
                for (Instance instance : reservation.instances()) {
                    if ("terminated".equals(instance.state().nameAsString())) {
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

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String instanceId = getInstanceId();

        sb.append("instance ");
        if (!ObjectUtils.isBlank(instanceId)) {
            sb.append(instanceId);
        }

        return sb.toString();
    }

    private void validate(Ec2Client client, boolean isCreate) {
        if (ObjectUtils.isBlank(getShutdownBehavior())
            || ShutdownBehavior.fromValue(getShutdownBehavior()).equals(ShutdownBehavior.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException("The value - (" + getShutdownBehavior() + ") is invalid for parameter Shutdown Behavior.");
        }

        if (ObjectUtils.isBlank(getInstanceType())
            || InstanceType.fromValue(getInstanceType()).equals(InstanceType.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException("The value - (" + getInstanceType() + ") is invalid for parameter Instance Type.");
        }

        if (!getEnableEnaSupport() && isCreate) {
            throw new BeamException("enableEnaSupport cannot be set to False at the time of instance creation. Update the instance to set it.");
        }

        if (!getSourceDestCheck() && isCreate) {
            throw new BeamException("SourceDestCheck cannot be set to False at the time of instance creation. Update the instance to set it.");
        }

        if (getSecurityGroupIds().isEmpty()) {
            throw new BeamException("At least one security group is required.");
        }

        if (!getCapacityReservation().equalsIgnoreCase("none")
            && !getCapacityReservation().equalsIgnoreCase("open")
            && !getCapacityReservation().startsWith("cr-")) {
            throw new BeamException("The value - (" + getCapacityReservation() + ") is invalid for parameter 'capacity-reservation'. "
                + "Valid values [ 'open', 'none', capacity reservation id like cr-% ]");
        }

        DescribeImagesRequest amiRequest;

        if (ObjectUtils.isBlank(getAmiId())) {
            if (ObjectUtils.isBlank(getAmiName())) {
                throw new BeamException("AMI name cannot be blank when AMI Id is not provided.");
            }

            amiRequest = DescribeImagesRequest.builder().filters(
                Collections.singletonList(Filter.builder().name("name").values(getAmiName()).build())
            ).build();

        } else {
            amiRequest = DescribeImagesRequest.builder().imageIds(getAmiId()).build();
        }

        try {
            DescribeImagesResponse response = client.describeImages(amiRequest);
            if (response.images().isEmpty()) {
                throw new BeamException("No AMI found for value - (" + getAmiName() + ") as an AMI Name.");
            }
            setAmiId(response.images().get(0).imageId());
        } catch (Ec2Exception ex) {
            if (ex.awsErrorDetails().errorCode().equalsIgnoreCase("InvalidAMIID.Malformed")) {
                throw new BeamException("No AMI found for value - (" + getAmiId() + ") as an AMI Id.");
            }

            throw ex;
        }
    }

    private void waitForRunningInstances(Ec2Client client) {
        // Wait for the instance to be not pending.
        boolean running = false;
        while (!running) {
            try {
                for (Reservation reservation : client.describeInstances(r -> r.instanceIds(getInstanceId())).reservations()) {
                    for (Instance instance : reservation.instances()) {
                        if ("running".equals(instance.state().nameAsString())) {
                            running = true;
                        }

                        setPublicDnsName(instance.publicDnsName());
                        setPublicIpAddress(instance.publicIpAddress());
                        setPrivateIpAddress(instance.privateIpAddress());
                        setInstanceState(instance.state().nameAsString());
                        setInstanceLaunchDate(Date.from(instance.launchTime()));
                    }
                }
            } catch (Ec2Exception error) {
                // Amazon sometimes doesn't make the instances available
                // immediately for API requests.
                if (!"InvalidInstanceID.NotFound".equals(error.awsErrorDetails().errorCode())) {
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

    private boolean validateInstanceStop(boolean instanceStopped, String param, String value) {
        if (!instanceStopped) {
            BeamCore.ui().write("\n@|bold,blue Skipping update of %s since instance"
                + " must be stopped to change parameter %s to %s|@", param, param, value);
            return false;
        }

        return true;
    }

    private boolean isInstanceStopped(Ec2Client client) {
        for (Reservation reservation : client.describeInstances(r -> r.instanceIds(getInstanceId())).reservations()) {
            for (Instance instance : reservation.instances()) {
                return ("stopped".equals(instance.state().nameAsString()));
            }

            return false;
        }

        return false;
    }

    private CapacityReservationSpecification getCapacityReservationSpecification() {
        if (getCapacityReservation().equals("none") || getCapacityReservation().equals("open")) {
            return CapacityReservationSpecification.builder()
                .capacityReservationPreference(getCapacityReservation().toLowerCase())
                .build();
        } else {
            return CapacityReservationSpecification.builder()
                .capacityReservationTarget(r -> r.capacityReservationId(getCapacityReservation()))
                .build();
        }
    }
}
