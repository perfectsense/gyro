package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import com.microsoft.azure.management.resources.ResourceGroupOperations;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroup;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ResourceGroupResource extends AzureResource<ResourceGroupExtended> {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String awsId() {
        return getName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, ResourceGroupExtended resourceGroup) {
        setName(resourceGroup.getName());
    }

    @Override
    public void create(AzureCloud cloud) {
        ResourceManagementClient rMclient = cloud.createResourceManagementClient();
        ResourceGroupOperations rGOperations = rMclient.getResourceGroupsOperations();

        try {
            ResourceGroup resourceGroup = new ResourceGroup();
            resourceGroup.setLocation(getRegion());
            rGOperations.createOrUpdate(getName(), resourceGroup);

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update resource group: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, ResourceGroupExtended> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AzureCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplayString() {
        return ("resource group " + getName());
    }

}
