package beam.openstack.config;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.domain.VolumeAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CinderResource extends OpenStackResource<Volume> {

    private String name;
    private String volumeType;
    private Integer size;
    private String deviceName;
    private boolean deleteOnTerminate;
    private String image;
    private String snapshotId;
    private Map<String, String> metadata;
    private boolean isBootDevice;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isDeleteOnTerminate() {
        return deleteOnTerminate;
    }

    public void setDeleteOnTerminate(boolean deleteOnTerminate) {
        this.deleteOnTerminate = deleteOnTerminate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public boolean isBootDevice() {
        return isBootDevice;
    }

    public void setBootDevice(boolean isBootDevice) {
        this.isBootDevice = isBootDevice;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getDeviceName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Volume volume) {
        setName(volume.getName());
        setVolumeType(volume.getVolumeType());
        setSize(volume.getSize());

        for (VolumeAttachment attachment : volume.getAttachments()) {
            setDeviceName(attachment.getDevice());
            break;
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {

    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Volume> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getSize());
        sb.append("GB ");
        sb.append(getVolumeType());
        sb.append(" ");

        sb.append("Cinder volume ");
        sb.append("attached to ");
        sb.append(getDeviceName());

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%sGB:%s:%s", getSize(), getVolumeType(), getDeviceName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CinderResource that = (CinderResource) o;

        return !(deviceName != null ? !deviceName.equals(that.deviceName) : that.deviceName != null);

    }

    @Override
    public int hashCode() {
        return deviceName != null ? deviceName.hashCode() : 0;
    }

}