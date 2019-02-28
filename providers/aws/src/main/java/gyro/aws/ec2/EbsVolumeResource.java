package gyro.aws.ec2;

import gyro.aws.AwsResource;
import gyro.core.BeamCore;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumeAttributeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttributeName;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * Creates a EBS Volume.
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     aws::ebs-volume ebs-volume-example
 *         availability-zone: "us-east-2a"
 *         size: 100
 *         auto-enable-io: false
 *         tags: {
 *             Name: "ebs-volume-example"
 *         }
 *     end
 */
@ResourceName("ebs-volume")
public class EbsVolumeResource extends Ec2TaggableResource<Volume> {

    private String availabilityZone;
    private Date createTime;
    private Boolean encrypted;
    private Integer iops;
    private String kmsKeyId;
    private Integer size;
    private String snapshotId;
    private String state;
    private String volumeId;
    private String volumeType;
    private Boolean autoEnableIo;

    /**
     * The availability zone for thew volume being created. (Required)
     */
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * Should the volume be encrypted. Defaults to false.
     */
    public Boolean getEncrypted() {
        if (encrypted == null) {
            encrypted = false;
        }

        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * The number of I/O operations per second (IOPS) to provision for the volume.
     * Only allowed when 'volume-type' set to 'iops'.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getIops() {
        return iops;
    }

    public void setIops(Integer iops) {
        this.iops = iops;
    }

    /**
     * The kms key id, when using encrypted volume.
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    /**
     * The size of the volume in GiBs.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * The snapshot from which to create the volume. Required only
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ResourceOutput
    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    /**
     * The type of volume being created. Defaults to 'gp2'.
     * Valid options [ 'gp2', 'io1', 'st1', 'sc1', 'standard'].
     */
    @ResourceDiffProperty(updatable = true)
    public String getVolumeType() {
        if (volumeType == null) {
            volumeType = "gp2";
        }

        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    /**
     * Auto Enable IO. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getAutoEnableIo() {
        if (autoEnableIo == null) {
            autoEnableIo = false;
        }
        return autoEnableIo;
    }

    public void setAutoEnableIo(Boolean autoEnableIo) {
        this.autoEnableIo = autoEnableIo;
    }

    @Override
    protected boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        Volume volume = getVolume(client);

        if (volume == null) {
            return false;
        }

        setAvailabilityZone(volume.availabilityZone());
        setCreateTime(Date.from(volume.createTime()));
        setEncrypted(volume.encrypted());
        setIops(volume.iops());
        setKmsKeyId(volume.kmsKeyId());
        setSize(volume.size());
        setSnapshotId(volume.snapshotId());
        setState(volume.stateAsString());
        setVolumeType(volume.volumeTypeAsString());

        DescribeVolumeAttributeResponse responseAutoEnableIo = client.describeVolumeAttribute(
            r -> r.volumeId(getVolumeId())
                .attribute(VolumeAttributeName.AUTO_ENABLE_IO)
        );

        setAutoEnableIo(responseAutoEnableIo.autoEnableIO().value());

        return true;
    }

    @Override
    protected String getId() {
        return getVolumeId();
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        validate(true);

        CreateVolumeResponse response = client.createVolume(
            r -> r.availabilityZone(getAvailabilityZone())
                .encrypted(getEncrypted())
                .iops(getVolumeType().equals("io1") ? getIops() : null)
                .kmsKeyId(getKmsKeyId())
                .size(getSize())
                .snapshotId(getSnapshotId())
                .volumeType(getVolumeType())
        );

        setVolumeId(response.volumeId());
        setCreateTime(Date.from(response.createTime()));
        setState(response.stateAsString());

        if (getAutoEnableIo()) {
            try {
                client.modifyVolumeAttribute(
                    r -> r.volumeId(getVolumeId())
                        .autoEnableIO(a -> a.value(getAutoEnableIo()))
                );
            } catch (Exception ex) {
                BeamCore.ui().write("\n@|bold,blue EBS Volume resource - error enabling "
                    + "'auto enable io' to volume with Id - %s. |@", getVolumeId());
                BeamCore.ui().write("\n@|bold,blue Error message - %s |@", ex.getMessage());
                BeamCore.ui().write("\n@|bold,blue Please retry to enable 'auto enable io' again. |@");
            }
        }
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        validate(false);

        if (changedProperties.contains("iops") || changedProperties.contains("size") || changedProperties.contains("volume-type")) {

            client.modifyVolume(
                r -> r.volumeId(getVolumeId())
                    .iops(getVolumeType().equals("io1") ? getIops() : null)
                    .size(getSize())
                    .volumeType(getVolumeType())
            );
        }

        if (changedProperties.contains("auto-enable-io")) {
            client.modifyVolumeAttribute(
                r -> r.volumeId(getVolumeId())
                    .autoEnableIO(a -> a.value(getAutoEnableIo()))
            );
        }
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteVolume(
            r -> r.volumeId(getVolumeId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ebs volume");

        if (!ObjectUtils.isBlank(getVolumeType())) {
            sb.append(" [ ").append(getVolumeType()).append(" ]");
        }

        if (!ObjectUtils.isBlank(getVolumeId())) {
            sb.append(" - ").append(getVolumeId());
        }

        return sb.toString();
    }

    private Volume getVolume(Ec2Client client) {
        if (ObjectUtils.isBlank(getVolumeId())) {
            throw new BeamException("ebs volume-id is missing, unable to load volume.");
        }

        try {
            DescribeVolumesResponse response = client.describeVolumes(
                r -> r.volumeIds(Collections.singleton(getVolumeId()))
            );

            if (response.volumes().isEmpty()) {
                return null;
            }

            return response.volumes().get(0);
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return null;
            }

            throw ex;
        }
    }

    private void validate(boolean isCreate) {
        if (!getVolumeType().equals("io1") && isCreate && getIops() != null) {
            throw new BeamException("The param 'iops' can only be set when param 'volume-type' is set to 'io1'.");
        }
    }
}
