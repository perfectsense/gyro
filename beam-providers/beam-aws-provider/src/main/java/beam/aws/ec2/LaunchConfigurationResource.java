package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.codec.binary.Base64;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingException;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResponse;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a Launch Configuration from config or an existing Instance Id.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::launch-configuration launch-configuration
 *         launch-configuration-name: "launch-configuration-beam-1"
 *         ami-id: "ami-01e24be29428c15b2"
 *         instance-type: "t2.micro"
 *         key-name: "instance-static"
 *         security-group-ids: [
 *             $(aws::security-group security-group-launch-configuration-example-1 | group-id),
 *             $(aws::security-group security-group-launch-configuration-example-2 | group-id)
 *         ]
 *         ebs-optimized: false
 *         enable-monitoring: true
 *         associate-public-ip: true
 *     end
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::launch-configuration launch-configuration
 *         launch-configuration-name: "launch-configuration-beam-1"
 *         instance-if: "i-0019ad12a990a504d"
 *         key-name: "instance-static"
 *         security-group-ids: [
 *             $(aws::security-group security-group-launch-configuration-example-1 | group-id),
 *             $(aws::security-group security-group-launch-configuration-example-2 | group-id)
 *         ]
 *         ebs-optimized: false
 *         enable-monitoring: true
 *         associate-public-ip: true
 *     end
 */
@ResourceName("launch-configuration")
public class LaunchConfigurationResource extends AwsResource {

    private String launchConfigurationName;

    public String getLaunchConfigurationName() {
        return launchConfigurationName;
    }

    public void setLaunchConfigurationName(String launchConfigurationName) {
        this.launchConfigurationName = launchConfigurationName;
    }

    private String instanceId;
    private String amiId;
    private String amiName;
    private Boolean ebsOptimized;
    private String instanceType;
    private String keyName;
    private Boolean enableMonitoring;
    private List<String> securityGroupIds;
    private String userData;
    private Boolean associatePublicIp;

    private String arn;

    /**
     * The ID of an launched instance that would be used as a skeleton to create the launch configuration. Required if AMI Name/ AMI ID not provided.
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

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public Boolean getAssociatePublicIp() {
        if (associatePublicIp == null) {
            associatePublicIp = false;
        }
        return associatePublicIp;
    }

    /**
     * Enable private Ip to intsances launched. See `Creating Launch Configuration <https://docs.aws.amazon.com/autoscaling/ec2/userguide/create-launch-config.html/>`_.
     */
    public void setAssociatePublicIp(Boolean associatePublicIp) {
        this.associatePublicIp = associatePublicIp;
    }

    @Override
    public boolean refresh() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        if (ObjectUtils.isBlank(getLaunchConfigurationName())) {
            throw new BeamException("launch-template-name is missing, unable to load instance.");
        }

        try {
            DescribeLaunchConfigurationsResponse response = client.describeLaunchConfigurations(
                r -> r.launchConfigurationNames(getLaunchConfigurationName())
            );

            for (LaunchConfiguration launchConfiguration : response.launchConfigurations()) {

                setArn(launchConfiguration.launchConfigurationARN());
                break;
            }
        } catch (AutoScalingException ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    public void create() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        // figure out a way to get the Ec2 client to work with this client.
        // Currently wont work with config set to AMI Name
        validate();

        client.createLaunchConfiguration(
            r -> r.launchConfigurationName(getLaunchConfigurationName())
                .ebsOptimized(getEbsOptimized())
                .imageId(ObjectUtils.isBlank(getInstanceId()) ? getAmiId() : null)
                .instanceMonitoring(o -> o.enabled(getEnableMonitoring()))
                .securityGroups(getSecurityGroupIds())
                .userData(new String(Base64.encodeBase64(getUserData().trim().getBytes())))
                .keyName(getKeyName())
                .instanceType(ObjectUtils.isBlank(getInstanceId()) ? getInstanceType() : null)
                .instanceId(ObjectUtils.isBlank(getInstanceId()) ? null : getInstanceId())
                .associatePublicIpAddress(getAssociatePublicIp())
        );
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        AutoScalingClient client = createClient(AutoScalingClient.class);

        client.deleteLaunchConfiguration(r -> r.launchConfigurationName(getLaunchConfigurationName()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Launch Configuration");

        if (!ObjectUtils.isBlank(getLaunchConfigurationName())) {
            sb.append(" - ").append(getLaunchConfigurationName());

        }

        return sb.toString();
    }

    private void validate() {
        if (ObjectUtils.isBlank(getInstanceId())) {
            if (InstanceType.fromValue(getInstanceType()).equals(InstanceType.UNKNOWN_TO_SDK_VERSION)) {
                throw new BeamException("The value - (" + getInstanceType() + ") is invalid for parameter Instance Type.");
            }

            if (getSecurityGroupIds().isEmpty()) {
                throw new BeamException("At least one security group is required.");
            }

            if (ObjectUtils.isBlank(getInstanceId())) {

                /*DescribeImagesRequest amiRequest;

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
                    Ec2Client ec2Client = createClient(Ec2Client.class);

                    DescribeImagesResponse response = ec2Client.describeImages(amiRequest);
                    if (response.images().isEmpty()) {
                        throw new BeamException("No AMI found for value - (" + getAmiName() + ") as an AMI Name.");
                    }
                    setAmiId(response.images().get(0).imageId());
                } catch (Ec2Exception ex) {
                    if (ex.awsErrorDetails().errorCode().equalsIgnoreCase("InvalidAMIID.Malformed")) {
                        throw new BeamException("No AMI found for value - (" + getAmiId() + ") as an AMI Id.");
                    }

                    throw ex;
                }*/
            }
        }
    }
}
