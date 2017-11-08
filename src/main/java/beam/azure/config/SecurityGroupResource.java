package beam.azure.config;

import java.util.*;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.NetworkSecurityGroupOperations;
import com.microsoft.azure.management.network.models.NetworkSecurityGroup;
import com.microsoft.azure.management.network.models.SecurityRule;
import com.psddev.dari.util.CompactMap;

public class SecurityGroupResource extends AzureResource<NetworkSecurityGroup> {

    private String id;
    private String groupName;
    private Set<SecurityRuleResource> ruleResources;

    private static final String NAME_KEY = "Name";
    private Map<String, String> tags;

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

    public Set<SecurityRuleResource> getRuleResources() {
        if (ruleResources == null) {
            ruleResources = new HashSet<>();
        }
        return ruleResources;
    }

    public void setRuleResources(Set<SecurityRuleResource> ruleResources) {
        this.ruleResources = ruleResources;
    }

    public String getName() {
        return getTags().get(NAME_KEY);
    }

    public void setName(String name) {
        if (name != null) {
            getTags().put(NAME_KEY, name);

        } else {
            getTags().remove(NAME_KEY);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getResourceGroup(), getClass().getName(), getGroupName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, NetworkSecurityGroup group) {
        setId(group.getId());
        setGroupName(group.getName());
        setTags(group.getTags());

        for (SecurityRule rule : group.getSecurityRules()) {
            if (!isInclude(filter, rule)) {
                continue;
            }

            SecurityRuleResource ruleResource = new SecurityRuleResource();
            ruleResource.setRegion(getRegion());
            ruleResource.setFromGroup(ruleResource.newReference(this));
            ruleResource.init(cloud, filter, rule);
            getRuleResources().add(ruleResource);
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getRuleResources());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, NetworkSecurityGroup> current) throws Exception {
        SecurityGroupResource currentSg = (SecurityGroupResource) current;
        update.update(currentSg.getRuleResources(), getRuleResources());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getRuleResources());
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkSecurityGroupOperations nSGOperations = client.getNetworkSecurityGroupsOperations();
        NetworkSecurityGroup group = new NetworkSecurityGroup();
        group.setName(getGroupName());
        group.setLocation(getRegion());

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        group.setTags(tags);

        try {
            nSGOperations.beginCreateOrUpdating(getResourceGroup(), getGroupName(), group);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update security group: " + getGroupName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, NetworkSecurityGroup> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkSecurityGroupOperations nSGOperations = client.getNetworkSecurityGroupsOperations();

        try {
            nSGOperations.beginDeleting(getResourceGroup(), getGroupName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete security group: " + getGroupName());
        }

    }

    @Override
    public String toDisplayString() {
        return "network security group: " + getGroupName();
    }
}

