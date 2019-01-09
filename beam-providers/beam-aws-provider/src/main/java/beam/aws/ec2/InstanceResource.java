package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.MonitoringState;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesMonitoringEnabled;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.ShutdownBehavior;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("instance")
public class InstanceResource extends Ec2TaggableResource<Instance> {

    private String instanceId;
    private String amiId;
    private String amiName;
    private int coreCount;
    private int threadPerCore;
    private Boolean ebsOptimized;
    private Boolean configureHibernateOption;
    private String shutdownBehavior;
    private String instanceType;
    private String keyName;
    private Boolean enableMonitoring;
    private List<String> securityGroupIds;
    private String subnetId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public String getAmiName() {
        return amiName;
    }

    public void setAmiName(String amiName) {
        this.amiName = amiName;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(int coreCount) {
        this.coreCount = coreCount;
    }

    public int getThreadPerCore() {
        return threadPerCore;
    }

    public void setThreadPerCore(int threadPerCore) {
        this.threadPerCore = threadPerCore;
    }

    public Boolean getEbsOptimized() {
        if (ebsOptimized == null) {
            ebsOptimized = false;
        }

        return ebsOptimized;
    }

    public void setEbsOptimized(Boolean ebsOptimized) {
        this.ebsOptimized = ebsOptimized;
    }

    public Boolean getConfigureHibernateOption() {
        if (configureHibernateOption == null) {
            configureHibernateOption = false;
        }

        return configureHibernateOption;
    }

    public void setConfigureHibernateOption(Boolean configureHibernateOption) {
        this.configureHibernateOption = configureHibernateOption;
    }

    public String getShutdownBehavior() {
        return shutdownBehavior != null ? shutdownBehavior.toLowerCase() : shutdownBehavior;
    }

    public void setShutdownBehavior(String shutdownBehavior) {
        this.shutdownBehavior = shutdownBehavior;
    }

    public String getInstanceType() {
        return instanceType != null ? instanceType.toLowerCase() : instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Boolean getEnableMonitoring() {
        if (enableMonitoring == null) {
            enableMonitoring = false;
        }
        return enableMonitoring;
    }

    public void setEnableMonitoring(Boolean enableMonitoring) {
        this.enableMonitoring = enableMonitoring;
    }

    public List<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(List<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
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
                    break;
                }
                break;
            }
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
        if (ShutdownBehavior.fromValue(getShutdownBehavior()).equals(ShutdownBehavior.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException("The value - (" + getShutdownBehavior() + ") is invalid for parameter Shutdown Behavior.");
        }

        if (InstanceType.fromValue(getInstanceType()).equals(InstanceType.UNKNOWN_TO_SDK_VERSION)) {
            throw new BeamException("The value - (" + getInstanceType() + ") is invalid for parameter Instance Type.");
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

        Ec2Client client = createClient(Ec2Client.class);

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

        try {
            RunInstancesResponse response = client.runInstances(
                r -> r.imageId(getAmiId())
                    .ebsOptimized(getEbsOptimized())
                    .hibernationOptions(o -> o.configured(getConfigureHibernateOption()))
                    .instanceInitiatedShutdownBehavior(getShutdownBehavior())
                    .cpuOptions(o -> o.build())
                    .instanceType(getInstanceType())
                    .keyName(getKeyName())
                    .maxCount(1)
                    .minCount(1)
                    .monitoring(RunInstancesMonitoringEnabled.builder().enabled(getEnableMonitoring()).build())
                    .securityGroupIds(getSecurityGroupIds())
                    .subnetId(getSubnetId())
            );

            setInstanceId(response.instances().get(0).instanceId());
        } catch (Ec2Exception ex) {
            throw ex;
        }
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.terminateInstances(r -> r.instanceIds(Collections.singletonList(getInstanceId())));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String instanceId = getInstanceId();

        if (!ObjectUtils.isBlank(instanceId)) {
            sb.append(instanceId);

        } else {
            sb.append("instance");
        }

        return sb.toString();
    }
}
