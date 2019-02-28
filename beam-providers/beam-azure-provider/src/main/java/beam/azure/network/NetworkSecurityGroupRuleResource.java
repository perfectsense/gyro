package beam.azure.network;

import beam.azure.AzureResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.SecurityRuleAccess;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ResourceName(parent = "network-security-group", value = "rule")
public class NetworkSecurityGroupRuleResource extends AzureResource {
    private String securityGroupRuleName;
    private Boolean inboundRule;
    private Boolean allowRule;
    private List<String> fromAddresses;
    private List<String> fromPorts;
    private List<String> toAddresses;
    private List<String> toPorts;
    private String fromApplicationSecurityGroupId;
    private String toApplicationSecurityGroupId;
    private String description;
    private Integer priority;
    private String protocol;

    private static final Map<String, SecurityRuleProtocol> protocolMap = ImmutableMap
        .of("all", SecurityRuleProtocol.ASTERISK,
            "tcp", SecurityRuleProtocol.TCP,
            "udp", SecurityRuleProtocol.UDP);

    /**
     * Name of the rule. (Required)
     */
    public String getSecurityGroupRuleName() {
        return securityGroupRuleName;
    }

    public void setSecurityGroupRuleName(String securityGroupRuleName) {
        this.securityGroupRuleName = securityGroupRuleName;
    }

    /**
     * Set rule type as inbound or outbound. Defaults to true i.e inbound. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getInboundRule() {
        if (inboundRule == null) {
            inboundRule = true;
        }

        return inboundRule;
    }

    public void setInboundRule(Boolean inboundRule) {
        this.inboundRule = inboundRule;
    }

    /**
     * Set rule to allow or block traffic. Defaults to true i.e allow. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getAllowRule() {
        if (allowRule == null) {
            allowRule = true;
        }

        return allowRule;
    }

    public void setAllowRule(Boolean allowRule) {
        this.allowRule = allowRule;
    }

    /**
     * A list of source addresses for the rule to work. Required if fromApplicationSecurityGroupId is not set.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getFromAddresses() {
        if (fromAddresses == null) {
            fromAddresses = new ArrayList<>();
        }

        Collections.sort(fromAddresses);

        return fromAddresses;
    }

    public void setFromAddresses(List<String> fromAddresses) {
        this.fromAddresses = fromAddresses;
    }

    /**
     * A list of source ports for the rule to work. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getFromPorts() {
        if (fromPorts == null) {
            fromPorts = new ArrayList<>();
        }

        Collections.sort(fromPorts);

        return fromPorts;
    }

    public void setFromPorts(List<String> fromPorts) {
        this.fromPorts = fromPorts;
    }

    /**
     * A list of destination addresses for the rule to work. Required if toApplicationSecurityGroupId is not set.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getToAddresses() {
        if (toAddresses == null) {
            toAddresses = new ArrayList<>();
        }

        Collections.sort(toAddresses);

        return toAddresses;
    }

    public void setToAddresses(List<String> toAddresses) {
        this.toAddresses = toAddresses;
    }

    /**
     * A list of destination ports for the rule to work. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getToPorts() {
        if (toPorts == null) {
            toPorts = new ArrayList<>();
        }

        Collections.sort(toPorts);

        return toPorts;
    }

    public void setToPorts(List<String> toPorts) {
        this.toPorts = toPorts;
    }

    /**
     * Source application security group id. Required if fromAddresses not set.
     */
    @ResourceDiffProperty(updatable = true)
    public String getFromApplicationSecurityGroupId() {
        return fromApplicationSecurityGroupId;
    }

    public void setFromApplicationSecurityGroupId(String fromApplicationSecurityGroupId) {
        this.fromApplicationSecurityGroupId = fromApplicationSecurityGroupId;
    }

    /**
     * Destination application security group id. Required if toAddresses not set.
     */
    @ResourceDiffProperty(updatable = true)
    public String getToApplicationSecurityGroupId() {
        return toApplicationSecurityGroupId;
    }

    public void setToApplicationSecurityGroupId(String toApplicationSecurityGroupId) {
        this.toApplicationSecurityGroupId = toApplicationSecurityGroupId;
    }

    /**
     * Description for the rule.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Priority for the rule. Valid values [ Integer 100 to 4096 ]. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Priority for the rule. Valid values [ all, tcp, udp ]. Defaults to all. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        if (protocol == null) {
            protocol = "all";
        }

        return protocol.toLowerCase();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public NetworkSecurityGroupRuleResource() {

    }

    public NetworkSecurityGroupRuleResource(NetworkSecurityRule networkSecurityRule) {
        setSecurityGroupRuleName(networkSecurityRule.name());
        setInboundRule(networkSecurityRule.direction().equals(SecurityRuleDirection.INBOUND));
        setAllowRule(networkSecurityRule.access().equals(SecurityRuleAccess.ALLOW));

        if (networkSecurityRule.sourceAddressPrefix() != null) {
            setFromAddresses(Collections.singletonList(networkSecurityRule.sourceAddressPrefix()));
        } else if (!networkSecurityRule.sourceAddressPrefixes().isEmpty()) {
            setFromAddresses(new ArrayList<>(networkSecurityRule.sourceAddressPrefixes()));
        }

        if (networkSecurityRule.sourcePortRange() != null) {
            setFromPorts(Collections.singletonList(networkSecurityRule.sourcePortRange()));
        } else if (!networkSecurityRule.sourcePortRanges().isEmpty()) {
            setFromPorts(new ArrayList<>(networkSecurityRule.sourcePortRanges()));
        }

        if (networkSecurityRule.destinationAddressPrefix() != null) {
            setToAddresses(Collections.singletonList(networkSecurityRule.destinationAddressPrefix()));
        } else if (!networkSecurityRule.destinationAddressPrefixes().isEmpty()) {
            setToAddresses(new ArrayList<>(networkSecurityRule.destinationAddressPrefixes()));
        }

        if (networkSecurityRule.destinationPortRange() != null) {
            setToPorts(Collections.singletonList(networkSecurityRule.destinationPortRange()));
        } else if (!networkSecurityRule.destinationPortRanges().isEmpty()) {
            setToPorts(new ArrayList<>(networkSecurityRule.destinationPortRanges()));
        }

        setDescription(networkSecurityRule.description());
        setPriority(networkSecurityRule.priority());
        setProtocol(networkSecurityRule.protocol().toString().equals("*") ? "all" : networkSecurityRule.protocol().toString());

        if (!networkSecurityRule.sourceApplicationSecurityGroupIds().isEmpty()) {
            setFromApplicationSecurityGroupId(networkSecurityRule.sourceApplicationSecurityGroupIds().iterator().next());
        }

        if (!networkSecurityRule.destinationApplicationSecurityGroupIds().isEmpty()) {
            setToApplicationSecurityGroupId(networkSecurityRule.destinationApplicationSecurityGroupIds().iterator().next());
        }
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        Azure client = createClient();

        NetworkSecurityGroupResource parent = (NetworkSecurityGroupResource) parent();

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups().getById(parent.getNetworkSecurityGroupId());

        NetworkSecurityRule.UpdateDefinitionStages
            .Blank<NetworkSecurityGroup.Update> updateBlank = networkSecurityGroup
            .update().defineRule(getSecurityGroupRuleName());

        NetworkSecurityRule.UpdateDefinitionStages.WithSourceAddressOrSecurityGroup<NetworkSecurityGroup.Update> withDirection;

        if (getInboundRule()) {
            withDirection = getAllowRule() ? updateBlank.allowInbound() : updateBlank.denyInbound();
        } else {
            withDirection = getAllowRule() ? updateBlank.allowOutbound() : updateBlank.denyOutbound();
        }

        NetworkSecurityRule.UpdateDefinitionStages.WithSourcePort<NetworkSecurityGroup.Update> withFromAddress;

        if (!ObjectUtils.isBlank(getFromApplicationSecurityGroupId())) {
            withFromAddress = withDirection.withSourceApplicationSecurityGroup(getFromApplicationSecurityGroupId());
        } else {
            if (getFromAddresses().size() == 1 && getFromAddresses().get(0).equals("*")) {
                withFromAddress = withDirection.fromAnyAddress();
            } else {
                withFromAddress = withDirection.fromAddresses(getFromAddresses().toArray(new String[0]));
            }
        }

        NetworkSecurityRule.UpdateDefinitionStages.WithDestinationAddressOrSecurityGroup<NetworkSecurityGroup.Update> withFromPorts;

        if (getFromPorts().size() == 1 && getFromPorts().get(0).equals("*")) {
            withFromPorts = withFromAddress.fromAnyPort();
        } else {
            withFromPorts = withFromAddress.fromPortRanges(getFromPorts().toArray(new String[0]));
        }

        NetworkSecurityRule.UpdateDefinitionStages.WithDestinationPort<NetworkSecurityGroup.Update> withToAddress;

        if (!ObjectUtils.isBlank(getToApplicationSecurityGroupId())) {
            withToAddress = withFromPorts.withDestinationApplicationSecurityGroup(getToApplicationSecurityGroupId());
        } else {
            if (getToAddresses().size() == 1 && getToAddresses().get(0).equals("*")) {
                withToAddress = withFromPorts.toAnyAddress();
            } else {
                withToAddress = withFromPorts.toAddresses(getToAddresses().toArray(new String[0]));
            }
        }

        NetworkSecurityRule.UpdateDefinitionStages.WithProtocol<NetworkSecurityGroup.Update> withToPorts;
        if (getToPorts().size() == 1 && getToPorts().get(0).equals("*")) {
            withToPorts = withToAddress.toAnyPort();
        } else {
            withToPorts = withToAddress.toPortRanges(getToPorts().toArray(new String[0]));
        }

        withToPorts
            .withProtocol(protocolMap.get(getProtocol()))
            .withDescription(getDescription())
            .withPriority(getPriority())
            .attach().apply();
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        NetworkSecurityGroupResource parent = (NetworkSecurityGroupResource) parent();

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups().getById(parent.getNetworkSecurityGroupId());

        NetworkSecurityRule.Update update = networkSecurityGroup.update().updateRule(getSecurityGroupRuleName());

        if (getInboundRule()) {
            update = getAllowRule() ? update.allowInbound() : update.denyInbound();
        } else {
            update = getAllowRule() ? update.allowOutbound() : update.denyOutbound();
        }

        if (!ObjectUtils.isBlank(getFromApplicationSecurityGroupId())) {
            update = update.withSourceApplicationSecurityGroup(getFromApplicationSecurityGroupId());
        } else {
            if (getFromAddresses().size() == 1 && getFromAddresses().get(0).equals("*")) {
                update = update.fromAnyAddress();
            } else {
                update = update.fromAddresses(getFromAddresses().toArray(new String[0]));
            }
        }

        if (getFromPorts().size() == 1 && getFromPorts().get(0).equals("*")) {
            update = update.fromAnyPort();
        } else {
            update = update.fromPortRanges(getFromPorts().toArray(new String[0]));
        }

        if (!ObjectUtils.isBlank(getToApplicationSecurityGroupId())) {
            update = update.withDestinationApplicationSecurityGroup(getToApplicationSecurityGroupId());
        } else {
            if (getToAddresses().size() == 1 && getToAddresses().get(0).equals("*")) {
                update = update.toAnyAddress();
            } else {
                update = update.toAddresses(getToAddresses().toArray(new String[0]));
            }
        }

        if (getToPorts().size() == 1 && getToPorts().get(0).equals("*")) {
            update = update.toAnyPort();
        } else {
            update = update.toPortRanges(getToPorts().toArray(new String[0]));
        }

        update
            .withProtocol(protocolMap.get(getProtocol()))
            .withDescription(getDescription())
            .withPriority(getPriority())
            .parent().apply();
    }

    @Override
    public void delete() {
        Azure client = createClient();

        NetworkSecurityGroupResource parent = (NetworkSecurityGroupResource) parent();

        NetworkSecurityGroup networkSecurityGroup = client.networkSecurityGroups().getById(parent.getNetworkSecurityGroupId());

        networkSecurityGroup.update().withoutRule(getSecurityGroupRuleName()).apply();
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Security group ");

        sb.append(getInboundRule() ? "inbound rule" : "outbound rule");

        if (!ObjectUtils.isBlank(getSecurityGroupRuleName())) {
            sb.append(" - ").append(getSecurityGroupRuleName());
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s", getSecurityGroupRuleName());
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }
}
