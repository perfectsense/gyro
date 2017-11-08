package beam.azure.config;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.ResourceDiffProperty;
import com.google.common.base.Joiner;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.SecurityRuleOperations;
import com.microsoft.azure.management.network.models.SecurityRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SecurityRuleResource extends AzureResource<SecurityRule> {

    private BeamReference fromGroup;
    private String name;
    private Integer priority;
    private String access;
    private String ipProtocol;
    private String ipRange;
    private String portRange;

    public BeamReference getFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(BeamReference fromGroup) {
        this.fromGroup = fromGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPortRange() {
        return portRange;
    }

    public void setPortRange(String portRange) {
        this.portRange = portRange;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIpProtocol() {
        if (ipProtocol == null) {
            ipProtocol = "*";
        }

        return ipProtocol;
    }

    public void setIpProtocol(String ipProtocol) {
        this.ipProtocol = ipProtocol;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(Joiner.
                on(',').
                useForNull("null").
                join(Arrays.asList(
                        getFromGroup(),
                        getRegion(),
                        getName())));
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, SecurityRule rule) {
        setName(rule.getName());
        setPriority(rule.getPriority());
        setAccess(rule.getAccess());
        setIpProtocol(rule.getProtocol());
        setIpRange(rule.getSourceAddressPrefix());
        setPortRange(rule.getSourcePortRange());
    }

    @Override
    public void create(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        SecurityRuleOperations sROperations = client.getSecurityRulesOperations();
        SecurityRule rule = new SecurityRule();
        rule.setAccess(getAccess());
        rule.setDestinationAddressPrefix("*");
        rule.setDestinationPortRange("*");
        rule.setDirection("InBound");
        rule.setSourceAddressPrefix(getIpRange());
        rule.setSourcePortRange(getPortRange());
        rule.setName(getName());
        rule.setProtocol(getIpProtocol());
        rule.setPriority(getPriority());

        SecurityGroupResource securityGroupResource = (SecurityGroupResource)getFromGroup().resolve();
        try {
            sROperations.createOrUpdate(getResourceGroup(), securityGroupResource.getGroupName(), rule.getName(), rule);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update security rule: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, SecurityRule> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        SecurityRuleOperations sROperations = client.getSecurityRulesOperations();

        SecurityGroupResource securityGroupResource = (SecurityGroupResource)getFromGroup().resolve();
        try {
            sROperations.delete(getResourceGroup(), securityGroupResource.getGroupName(), getName());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete security rule: " + getName());
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("security rule - ");
        sb.append(getAccess());
        sb.append(" ");

        String ipProtocol = getIpProtocol();

        if ("*".equals(ipProtocol)) {
            sb.append("all traffic ");

        } else {
            sb.append(ipProtocol);
            sb.append("traffic ");
        }

        sb.append("through ");
        String port = getPortRange();

        if ("*".equals(port)) {
            sb.append("all ports");

        } else {
            sb.append(port);
        }

        String ipRange = getIpRange();

        if ("*".equals(ipRange)) {
            sb.append(" from everywhere");

        } else {
            sb.append(" from ");
            sb.append(ipRange);
        }

        return sb.toString();
    }
}
