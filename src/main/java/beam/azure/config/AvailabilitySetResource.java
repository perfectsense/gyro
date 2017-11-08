package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.compute.AvailabilitySetOperations;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.models.AvailabilitySet;
import com.psddev.dari.util.CompactMap;

import java.util.*;

public class AvailabilitySetResource extends AzureResource<AvailabilitySet> {
    private String id;
    private String name;
    private Integer faultDomainCount;
    private Integer updateDomainCount;
    private Map<String, String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getFaultDomainCount() {
        return faultDomainCount;
    }

    public void setFaultDomainCount(Integer faultDomainCount) {
        this.faultDomainCount = faultDomainCount;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getUpdateDomainCount() {
        return updateDomainCount;
    }

    public void setUpdateDomainCount(Integer updateDomainCount) {
        this.updateDomainCount = updateDomainCount;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getResourceGroup(), getClass().getName(), getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, AvailabilitySet as) {
        setName(as.getName());
        setTags(as.getTags());
        setId(as.getId());
        setFaultDomainCount(as.getPlatformFaultDomainCount());
        setUpdateDomainCount(as.getPlatformUpdateDomainCount());
    }

    @Override
    public void create(AzureCloud cloud) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        AvailabilitySetOperations aSOperations = client.getAvailabilitySetsOperations();
        AvailabilitySet as = new AvailabilitySet();
        as.setName(getName());
        as.setLocation(getRegion());
        as.setPlatformFaultDomainCount(getFaultDomainCount());
        as.setPlatformUpdateDomainCount(getUpdateDomainCount());

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        as.setTags(tags);

        try {
            aSOperations.createOrUpdate(getResourceGroup(), as);

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update availability set: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, AvailabilitySet> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        AvailabilitySetOperations aSOperations = client.getAvailabilitySetsOperations();

        try {
            aSOperations.delete(getResourceGroup(), getName());

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete availability set: " + getName());
        }
    }

    public String getIdFromName(AzureCloud cloud) {
        return String.format("%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Compute",
                "/availabilitySets/", getName());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("availability set ");
        sb.append(getName());

        return sb.toString();
    }
}
