package beam.aws.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class EbsResource extends TaggableEC2Resource<Volume> {

    private BeamReference instance;
    private String availabilityZone;
    private Boolean encrypted;
    private Integer iops;
    private Integer size;
    private String snapshotId;
    private String volumeId;
    private String volumeType;
    private String deviceName;
    private Boolean deleteOnTerminate;
    private Boolean copiedFromImage;

    public BeamReference getInstance() {
        return newParentReference(InstanceResource.class, instance);
    }

    public void setInstance(BeamReference instance) {
        this.instance = instance;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Integer getIops() {
        return iops;
    }

    public void setIops(Integer iops) {
        this.iops = iops;
    }

    @ResourceDiffProperty
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    @ResourceDiffProperty
    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getDeleteOnTerminate() {
        return deleteOnTerminate;
    }

    public void setDeleteOnTerminate(Boolean deleteOnTerminate) {
        this.deleteOnTerminate = deleteOnTerminate;
    }

    public Boolean isCopiedFromImage() {
        return copiedFromImage;
    }

    public void setCopiedFromImage(Boolean copiedFromImage) {
        this.copiedFromImage = copiedFromImage;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public String awsId() {
        return getVolumeId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getDeviceName());
    }

    @Override
    public void doInit(AWSCloud cloud, BeamResourceFilter filter, Volume awsResource) {
        setAvailabilityZone(awsResource.getAvailabilityZone());
        setEncrypted(awsResource.getEncrypted());
        setIops(awsResource.getIops());
        setSize(awsResource.getSize());
        setSnapshotId(awsResource.getSnapshotId());
        setVolumeId(awsResource.getVolumeId());
        setVolumeType(awsResource.getVolumeType());

        for (VolumeAttachment attachment : awsResource.getAttachments()) {
            setDeleteOnTerminate(attachment.getDeleteOnTermination());
            setDeviceName(attachment.getDevice());
        }
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        if (getAvailabilityZone() != null) {
            AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
            CreateVolumeRequest cvRequest = new CreateVolumeRequest();
            cvRequest.setSize(getSize());
            cvRequest.setIops(getIops());
            cvRequest.setEncrypted(getEncrypted());
            cvRequest.setAvailabilityZone(getAvailabilityZone());
            cvRequest.setVolumeType(getVolumeType());

            CreateVolumeResult result = client.createVolume(cvRequest);
            setVolumeId(result.getVolume().getVolumeId());

            // Wait for all volumes to be available
            boolean available = false;
            while (!available) {
                DescribeVolumesRequest dvRequest = new DescribeVolumesRequest();
                dvRequest.setVolumeIds(Lists.newArrayList(getVolumeId()));

                DescribeVolumesResult dvResult = client.describeVolumes(dvRequest);
                for (Volume volume : dvResult.getVolumes()) {
                    if (volume.getState().equals("available")) {
                        available = true;
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException error) {
                    return;
                }
            }

            // Attach volumes
            InstanceResource instanceResource = (InstanceResource) getInstance().resolve();

            AttachVolumeRequest avRequest = new AttachVolumeRequest();
            avRequest.setVolumeId(getVolumeId());
            avRequest.setInstanceId(instanceResource.awsId());
            avRequest.setDevice(getDeviceName());
            client.attachVolume(avRequest);

            // Change deleteOnTerminate for volumes.
            InstanceBlockDeviceMappingSpecification spec = new InstanceBlockDeviceMappingSpecification();
            spec.setDeviceName(getDeviceName());

            EbsInstanceBlockDeviceSpecification ebsSpec = new EbsInstanceBlockDeviceSpecification();
            ebsSpec.setDeleteOnTermination(getDeleteOnTerminate());
            ebsSpec.setVolumeId(getVolumeId());
            spec.setEbs(ebsSpec);

            ModifyInstanceAttributeRequest miaRequest = new ModifyInstanceAttributeRequest();
            miaRequest.setInstanceId(instanceResource.awsId());
            miaRequest.setBlockDeviceMappings(Lists.newArrayList(spec));
            client.modifyInstanceAttribute(miaRequest);
        } else {
            AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
            DescribeVolumesRequest dvRequest = new DescribeVolumesRequest();
            dvRequest.setFilters(Arrays.asList(
                    new Filter("attachment.instance-id").withValues(Arrays.asList(getInstance().awsId())),
                    new Filter("attachment.device").withValues(Arrays.asList(getDeviceName()))));

            try {
                Volume volume = client.describeVolumes(dvRequest).getVolumes().get(0);
                setVolumeId(volume.getVolumeId());
            } catch (Exception error) {
                throw new BeamException("Error with tagging EBS volumes created from AMI snapshots: " + error.getMessage());
            }
        }
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<Volume> config, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        // Change deleteOnTerminate for volumes.
        InstanceBlockDeviceMappingSpecification spec = new InstanceBlockDeviceMappingSpecification();
        spec.setDeviceName(getDeviceName());

        EbsInstanceBlockDeviceSpecification ebsSpec = new EbsInstanceBlockDeviceSpecification();
        ebsSpec.setDeleteOnTermination(getDeleteOnTerminate());
        ebsSpec.setVolumeId(getVolumeId());
        spec.setEbs(ebsSpec);

        ModifyInstanceAttributeRequest miaRequest = new ModifyInstanceAttributeRequest();
        miaRequest.setInstanceId(getInstance().awsId());
        miaRequest.setBlockDeviceMappings(Lists.newArrayList(spec));
        miaRequest.setAttribute(InstanceAttributeName.BlockDeviceMapping);
        client.modifyInstanceAttribute(miaRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getSize());
        sb.append("GB ");

        if (getVolumeType().equals("gp2")) {
            sb.append("SSD ");
        } else if (getVolumeType().equals("standard")) {
            sb.append("Magnetic ");
        } else if (getVolumeType().equals("io1")) {
            sb.append("Provisioned IOPS ");
        }

        sb.append("EBS volume ");
        sb.append("attached to ");
        sb.append(getDeviceName());

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%sGB:%s:%s", getSize(), getVolumeType(), getDeviceName());
    }

}