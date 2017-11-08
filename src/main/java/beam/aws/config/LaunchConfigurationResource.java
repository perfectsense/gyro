package beam.aws.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AlreadyExistsException;
import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;

public class LaunchConfigurationResource extends AWSResource<LaunchConfiguration> {

    private Boolean associatePublicIpAddress;
    private List<BlockDeviceMapping> blockDeviceMappings;
    private Boolean ebsOptimized;
    private BeamReference iamInstanceProfile;
    private String imageId;
    private String instanceType;
    private String kernelId;
    private String keyName;
    private String launchConfigurationName;
    private String placementTenancy;
    private String ramdiskId;
    private Set<BeamReference> securityGroups;
    private String spotPrice;
    private String userData;
    private String imageName;

    @ResourceDiffProperty
    public Boolean getAssociatePublicIpAddress() {
        return associatePublicIpAddress;
    }

    public void setAssociatePublicIpAddress(Boolean associatePublicIpAddress) {
        this.associatePublicIpAddress = associatePublicIpAddress;
    }

    /**
     * @return Never {@code null}.
     */
    public List<BlockDeviceMapping> getBlockDeviceMappings() {
        if (blockDeviceMappings == null) {
            blockDeviceMappings = new ArrayList<>();
        }
        return blockDeviceMappings;
    }

    public void setBlockDeviceMappings(List<BlockDeviceMapping> blockDeviceMappings) {
        this.blockDeviceMappings = blockDeviceMappings;
    }

    @ResourceDiffProperty
    public Boolean getEbsOptimized() {
        return ebsOptimized;
    }

    public void setEbsOptimized(Boolean ebsOptimized) {
        this.ebsOptimized = ebsOptimized;
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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @ResourceDiffProperty
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

    public String getLaunchConfigurationName() {
        return launchConfigurationName;
    }

    public void setLaunchConfigurationName(String launchConfigurationName) {
        this.launchConfigurationName = launchConfigurationName;
    }

    @ResourceDiffProperty
    public String getPlacementTenancy() {
        return placementTenancy;
    }

    public void setPlacementTenancy(String placementTenancy) {
        this.placementTenancy = placementTenancy;
    }

    @ResourceDiffProperty
    public String getRamdiskId() {
        return ramdiskId;
    }

    public void setRamdiskId(String ramdiskId) {
        this.ramdiskId = ramdiskId;
    }

    /**
     * @return Never {@code null}.
     */
    @ResourceDiffProperty
    public Set<BeamReference> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new NullSet<>();
        }
        return securityGroups;
    }

    public void setSecurityGroups(Set<BeamReference> securityGroups) {
        this.securityGroups = securityGroups;
    }

    @ResourceDiffProperty
    public String getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(String spotPrice) {
        this.spotPrice = spotPrice;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    @Override
    public String awsId() {
        return getLaunchConfigurationName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getLaunchConfigurationName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, LaunchConfiguration group) {
        setAssociatePublicIpAddress(group.getAssociatePublicIpAddress());
        setBlockDeviceMappings(group.getBlockDeviceMappings());
        setEbsOptimized(group.getEbsOptimized());
        setIamInstanceProfile(newReference(InstanceProfileResource.class, group.getIamInstanceProfile()));
        setImageId(group.getImageId());
        setInstanceType(group.getInstanceType());
        setKernelId(group.getKernelId());
        setKeyName(group.getKeyName());
        setLaunchConfigurationName(group.getLaunchConfigurationName());
        setPlacementTenancy(group.getPlacementTenancy());
        setRamdiskId(group.getRamdiskId());
        setSecurityGroups(newReferenceSet(SecurityGroupResource.class, group.getSecurityGroups()));
        setSpotPrice(group.getSpotPrice());
        setUserData(group.getUserData());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        CreateLaunchConfigurationRequest clcRequest = new CreateLaunchConfigurationRequest();

        clcRequest.setAssociatePublicIpAddress(getAssociatePublicIpAddress());

        List<BlockDeviceMapping> mappings = getBlockDeviceMappings();

        if (!mappings.isEmpty()) {
            clcRequest.setBlockDeviceMappings(getBlockDeviceMappings());
        }

        clcRequest.setEbsOptimized(getEbsOptimized());
        clcRequest.setIamInstanceProfile(getIamInstanceProfile().awsId());
        clcRequest.setImageId(getImageId());
        clcRequest.setInstanceType(getInstanceType());
        clcRequest.setKernelId(getKernelId());
        clcRequest.setKeyName(getKeyName());
        clcRequest.setLaunchConfigurationName(getLaunchConfigurationName());
        clcRequest.setPlacementTenancy(getPlacementTenancy());
        clcRequest.setSecurityGroups(awsIdSet(getSecurityGroups()));
        clcRequest.setSpotPrice(getSpotPrice());
        clcRequest.setUserData(getUserData());

        try {
            client.createLaunchConfiguration(clcRequest);
        } catch (AlreadyExistsException ase) {
            System.out.print(" [SKIPPING] ");
        } catch (Exception error) {
            executeService(() -> {
                client.createLaunchConfiguration(clcRequest);
                return null;
            });
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, LaunchConfiguration> current, Set<String> changedProperties) {
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        DeleteLaunchConfigurationRequest dlcRequest = new DeleteLaunchConfigurationRequest();

        dlcRequest.setLaunchConfigurationName(getLaunchConfigurationName());
        client.deleteLaunchConfiguration(dlcRequest);
    }

    @Override
    public String toDisplayString() {
        return "launch configuration " + getLaunchConfigurationName();
    }
}
