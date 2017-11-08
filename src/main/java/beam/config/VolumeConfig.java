package beam.config;

public class VolumeConfig {

    private String name;
    private String availabilityZone;
    private Boolean encrypted;
    private Integer iops;
    private Integer size;
    private String snapshotId;
    private String image;
    private String volumeType;
    private String deviceName;
    private Boolean deleteOnTerminate;
    private Integer lun;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

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

    public Boolean getDeleteOnTerminate() {
        if (deleteOnTerminate == null) {
            deleteOnTerminate = true;
        }

        return deleteOnTerminate;
    }

    public void setDeleteOnTerminate(Boolean deleteOnTerminate) {
        this.deleteOnTerminate = deleteOnTerminate;
    }

    public Integer getLun() {
        return lun;
    }

    public void setLun(Integer lun) {
        this.lun = lun;
    }
}