package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.core.diff.ResourceOutput;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.codec.binary.Base64;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplatesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.GetLaunchTemplateDataResponse;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.ShutdownBehavior;
import software.amazon.awssdk.utils.builder.SdkBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Creates a Launch Template from config or an existing Instance Id.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::launch-template launch-template-1
 *         launch-template-name: "launch-template-beam-1"
 *         ami-name: "amzn-ami-hvm-2018.03.0.20181129-x86_64-gp2"
 *         shutdown-behavior: "STOP"
 *         instance-type: "t2.micro"
 *         key-name: "instance-static"
 *         security-group-ids: [
 *             $(aws::security-group security-group-launch-template-example-1 | group-id),
 *             $(aws::security-group security-group-launch-template-example-2 | group-id)
 *         ]
 *         disable-api-termination: false
 *         ebs-optimized: false
 *
 *         tags: {
 *             Name: "launch-template-example-1"
 *         }
 *     end
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::launch-template launch-template-2
 *         launch-template-name: "launch-template-beam-2"
 *         instance-id : "in-instanceid"
 *
 *         tags: {
 *             Name: "launch-template-example-2"
 *         }
 *     end
 */
@ResourceName("launch-template")
public class LaunchTemplateResource extends Ec2TaggableResource<LaunchTemplate> {

    private String launchTemplateId;
    private String launchTemplateName;

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
    private Boolean disableApiTermination;
    private String userData;

    private String instanceId;

    @ResourceOutput
    public String getLaunchTemplateId() {
        return launchTemplateId;
    }

    public void setLaunchTemplateId(String launchTemplateId) {
        this.launchTemplateId = launchTemplateId;
    }

    public String getLaunchTemplateName() {
        return launchTemplateName;
    }

    public void setLaunchTemplateName(String launchTemplateName) {
        this.launchTemplateName = launchTemplateName;
    }

    /**
     * The ID of an AMI that would be used to launch the instance. Required if AMI Name not provided. See `Finding an AMI <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html/>`_.
     */
    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    /**
     * The Name of an AMI that would be used to launch the instance. Required if AMI Id not provided. See `Amazon Machine Images (AMI) <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html/>`_.
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
    public String getShutdownBehavior() {
        return shutdownBehavior != null ? shutdownBehavior.toLowerCase() : ShutdownBehavior.STOP.toString();
    }

    public void setShutdownBehavior(String shutdownBehavior) {
        this.shutdownBehavior = shutdownBehavior;
    }

    /**
     * Launch instance with the type of hardware you desire. See `Instance Types <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html/>`_. (Required)
     */
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
     * Enable or Disable api termination of an instance. See `Enabling Termination Protection for an Instance <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/terminating-instances.html#Using_ChangingDisableAPITermination/>`_.
     */
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
     * Set user data for your instance. See `Instance Metadata and User Data <https://docs.aws.amazon.com/AWSEC2/latest/WindowsGuide/ec2-instance-metadata.html/>`_.
     */
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

    /**
     * The id of the instance from which the details of the launch template will be extracted and used to make this one.
     */
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected String getId() {
        return getLaunchTemplateId();
    }

    @Override
    protected boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getLaunchTemplateId())) {
            throw new BeamException("launch-template-id is missing, unable to load instance.");
        }

        try {
            DescribeLaunchTemplatesResponse response = client.describeLaunchTemplates(r -> r.launchTemplateIds(getLaunchTemplateId()));
            for (LaunchTemplate launchTemplate : response.launchTemplates()) {
                setLaunchTemplateName(launchTemplate.launchTemplateName());
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
        Ec2Client client = createClient(Ec2Client.class);

        validate(client);

        if (!ObjectUtils.isBlank(getInstanceId())) {
            GetLaunchTemplateDataResponse response = client.getLaunchTemplateData(r -> r.instanceId(getInstanceId()));
            setDisableApiTermination(response.launchTemplateData().disableApiTermination());
            setEbsOptimized(response.launchTemplateData().ebsOptimized());
            setConfigureHibernateOption(response.launchTemplateData().hibernationOptions().configured());
            setAmiId(response.launchTemplateData().imageId());
            setInstanceType(response.launchTemplateData().instanceType().toString());
            setShutdownBehavior(response.launchTemplateData().instanceInitiatedShutdownBehaviorAsString());
            setKeyName(response.launchTemplateData().keyName());
            setEnableMonitoring(response.launchTemplateData().monitoring().enabled());
            setSecurityGroupIds(response.launchTemplateData().securityGroupIds());
            setUserData(response.launchTemplateData().userData());

            //temp fix until we resolve a way to verify by instance types.
            //setCoreCount(response.launchTemplateData().cpuOptions().coreCount());
            //setThreadPerCore(response.launchTemplateData().cpuOptions().threadsPerCore());
        }

        CreateLaunchTemplateResponse response = client.createLaunchTemplate(
            r -> r.launchTemplateName(getLaunchTemplateName())
            .launchTemplateData(
                l -> l.cpuOptions(getCoreCount() > 0
                    ? o -> o.threadsPerCore(getThreadPerCore()).coreCount(getCoreCount()).build() : SdkBuilder::build)
                    .disableApiTermination(getDisableApiTermination())
                    .ebsOptimized(getEbsOptimized())
                    .hibernationOptions(o -> o.configured(getConfigureHibernateOption()))
                    .imageId(getAmiId())
                    .instanceType(getInstanceType())
                    .instanceInitiatedShutdownBehavior(getShutdownBehavior())
                    .keyName(getKeyName())
                    .monitoring(o -> o.enabled(getEnableMonitoring()))
                    .securityGroupIds(getSecurityGroupIds())
                    .userData(new String(Base64.encodeBase64(getUserData().trim().getBytes())))
            )
        );

        setLaunchTemplateId(response.launchTemplate().launchTemplateId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteLaunchTemplate(r -> r.launchTemplateId(getLaunchTemplateId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String launchTemplateId = getLaunchTemplateId();

        sb.append("launch template");

        if (!ObjectUtils.isBlank(launchTemplateId)) {
            sb.append(" - ").append(launchTemplateId);

        }

        return sb.toString();
    }

    private void validate(Ec2Client client) {

        if (ObjectUtils.isBlank(getInstanceId())) {

            if (ObjectUtils.isBlank(getInstanceType()) || InstanceType.fromValue(getInstanceType()).equals(InstanceType.UNKNOWN_TO_SDK_VERSION)) {
                throw new BeamException("The value - (" + getInstanceType() + ") is invalid for parameter Instance Type.");
            }

            if (getSecurityGroupIds().isEmpty()) {
                throw new BeamException("At least one security group is required.");
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
    }
}
