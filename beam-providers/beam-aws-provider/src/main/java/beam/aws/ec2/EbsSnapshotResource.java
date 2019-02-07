package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateSnapshotResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Snapshot;

import java.util.Date;
import java.util.Set;

/**
 * Creates a EBS Snapshot based on the specified EBS volume.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::ebs-snapshot ebs-snapshot-example
 *         description: "ebs-snapshot-example"
 *         volume-id: "vol-0bda07cf6a43384e9"
 *         tags: {
 *             Name: 'ebs-snapshot-example'
 *         }
 *     end
 */
@ResourceName("ebs-snapshot")
public class EbsSnapshotResource extends Ec2TaggableResource<Snapshot> {

    private String volumeId;
    private String description;
    private String snapshotId;

    private String dataEncryptionKeyId;
    private Boolean encrypted;
    private String kmsKeyId;
    private String ownerAlias;
    private String ownerId;
    private String progress;
    private Date startTime;
    private String state;
    private String stateMessage;
    private Integer volumeSize;

    /**
     * The volume id based on which the snapshot would be created. (Required)
     */
    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    /**
     * The description for the snapshot.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getDataEncryptionKeyId() {
        return dataEncryptionKeyId;
    }

    public void setDataEncryptionKeyId(String dataEncryptionKeyId) {
        this.dataEncryptionKeyId = dataEncryptionKeyId;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    public String getOwnerAlias() {
        return ownerAlias;
    }

    public void setOwnerAlias(String ownerAlias) {
        this.ownerAlias = ownerAlias;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    @Override
    protected boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        Snapshot snapshot = getSnapshot(client);

        if (snapshot == null) {
            return false;
        }

        setDataEncryptionKeyId(snapshot.dataEncryptionKeyId());
        setDescription(snapshot.description());
        setEncrypted(snapshot.encrypted());
        setKmsKeyId(snapshot.kmsKeyId());
        setOwnerAlias(snapshot.ownerAlias());
        setOwnerId(snapshot.ownerId());
        setProgress(snapshot.progress());
        setStartTime(Date.from(snapshot.startTime()));
        setState(snapshot.stateAsString());
        setStateMessage(snapshot.stateMessage());
        setVolumeSize(snapshot.volumeSize());

        return true;
    }

    @Override
    protected String getId() {
        return getSnapshotId();
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateSnapshotResponse response = client.createSnapshot(
            r -> r.description(getDescription())
                .volumeId(getVolumeId())
        );

        setSnapshotId(response.snapshotId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteSnapshot(
            r -> r.snapshotId(getSnapshotId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ebs snapshot");

        if (!ObjectUtils.isBlank(getDescription())) {
            sb.append(" [ ").append(getDescription()).append(" ]");
        }

        return sb.toString();
    }

    private Snapshot getSnapshot(Ec2Client client) {
        if (ObjectUtils.isBlank(getSnapshotId())) {
            throw new BeamException("ebs snapshot-id is missing, unable to load snapshot.");
        }

        try {
            DescribeSnapshotsResponse response = client.describeSnapshots(
                r -> r.snapshotIds(getSnapshotId())
            );

            if (response.snapshots().isEmpty()) {
                return null;
            }

            return response.snapshots().get(0);
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return null;
            }

            throw ex;
        }
    }
}
