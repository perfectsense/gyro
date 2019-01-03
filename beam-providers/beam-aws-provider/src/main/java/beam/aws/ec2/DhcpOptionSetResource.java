package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DhcpConfiguration;
import software.amazon.awssdk.services.ec2.model.DhcpOptions;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.NewDhcpConfiguration;
import software.amazon.awssdk.services.ec2.model.Vpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Creates a DHCP option set with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::dhcp-option example-dhcp
 *         domain-name: [example.com]
 *         domain-name-servers: [192.168.1.1, 192.168.1.2]
 *         ntp-servers: [10.2.5.1]
 *         netbios-name-servers: [192.168.1.1, 192.168.1.2]
 *         netbios-node-type: [2]
 *     end
 */

@ResourceName("dhcp-option")
public class DhcpOptionSetResource extends Ec2TaggableResource<Vpc> {

    private String vpcId;
    private String dhcpOptionsId;
    private List<String> domainName;
    private List<String> domainNameServers;
    private List<String> ntpServers;
    private List<String> netbiosNameServers;
    private List<String> netbiosNodeType;

    /**
     * The id of the VPC to create a dhcp option for.
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    /**
     * The ID of a custom DHCP option set. See `DHCP Options Sets <https://docs.aws.amazon.com/vpc/latest/userguide/VPC_DHCP_Options.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDhcpOptionsId() {
        return dhcpOptionsId;
    }

    public void setDhcpOptionsId(String dhcpOptionsId) {
        this.dhcpOptionsId = dhcpOptionsId;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getDomainName() {
        if (domainName == null) {
            domainName = new ArrayList<>();
        }

        return domainName;
    }

    public void setDomainName(List<String> domainName) {
        this.domainName = domainName;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getDomainNameServers() {
        if (domainNameServers == null) {
            domainNameServers = new ArrayList<>();
        }

        return domainNameServers;
    }

    public void setDomainNameServers(List<String> domainNameServers) {
        this.domainNameServers = domainNameServers;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getNtpServers() {
        if (ntpServers == null) {
            ntpServers = new ArrayList<>();
        }

        return ntpServers;
    }

    public void setNtpServers(List<String> ntpServers) {
        this.ntpServers = ntpServers;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getNetbiosNameServers() {
        if (netbiosNameServers == null) {
            netbiosNameServers = new ArrayList<>();
        }

        return netbiosNameServers;
    }

    public void setNetbiosNameServers(List<String> netbiosNameServers) {
        this.netbiosNameServers = netbiosNameServers;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getNetbiosNodeType() {
        if (netbiosNodeType == null) {
            netbiosNodeType = new ArrayList<>();
        }

        return netbiosNodeType;
    }

    @ResourceDiffProperty(updatable = true)
    public void setNetbiosNodeType(List<String> netbiosNodeType) {
        this.netbiosNodeType = netbiosNodeType;
    }

    @Override
    protected String getId() {
        return getDhcpOptionsId();
    }

    public List<String> convertString(Collection<AttributeValue> toConvert) {
        List<String> convertedList = new ArrayList<>();
        for (AttributeValue str : toConvert) {
            convertedList.add(str.value());
        }
        return convertedList;
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeDhcpOptionsRequest request = DescribeDhcpOptionsRequest.builder()
                .dhcpOptionsIds(getDhcpOptionsId())
                .build();

        for (DhcpOptions options : client.describeDhcpOptions(request).dhcpOptions()) {
            String optionsId = options.dhcpOptionsId();
            setDhcpOptionsId(optionsId);

            for (DhcpConfiguration config : options.dhcpConfigurations()) {
                if (config.key().equals("domain-name")) {
                    setDomainName(convertString(config.values()));
                }
                if (config.key().equals("domain-name-servers")) {
                    setDomainNameServers(convertString(config.values()));
                }
                if (config.key().equals("ntp-servers")) {
                    setNtpServers(convertString(config.values()));
                }
                if (config.key().equals("netbios-name-servers")) {
                    setNetbiosNameServers(convertString(config.values()));
                }
                if (config.key().equals("netbios-node-type")) {
                    setNetbiosNodeType(convertString(config.values()));
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected void doCreate() {
        Collection<NewDhcpConfiguration> configs = new ArrayList<>();
        addDhcpConfiguration(configs, "domain-name", getDomainName());
        addDhcpConfiguration(configs, "domain-name-servers", getDomainNameServers());
        addDhcpConfiguration(configs, "ntp-servers", getNtpServers());
        addDhcpConfiguration(configs, "netbios-name-servers", getNetbiosNameServers());
        addDhcpConfiguration(configs, "netbios-node-type", getNetbiosNodeType());

        Ec2Client client = createClient(Ec2Client.class);

        CreateDhcpOptionsResponse response = client.createDhcpOptions(
            r -> r.dhcpConfigurations(configs)
        );

        String optionsId = response.dhcpOptions().dhcpOptionsId();
        setDhcpOptionsId(optionsId);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        String pastOptionId = getDhcpOptionsId();
        doCreate();
        deleteOption(pastOptionId);
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);
        try {
            client.deleteDhcpOptions(r -> r.dhcpOptionsId(getDhcpOptionsId()));

        } catch (Ec2Exception err) {
            throw new BeamException("This option set has dependencies and cannot be deleted.");
        }
    }

    public void deleteOption(String optionsId) {
        Ec2Client client = createClient(Ec2Client.class);
        client.deleteDhcpOptions(r -> r.dhcpOptionsId(getDhcpOptionsId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getDhcpOptionsId() != null) {
            sb.append(getDhcpOptionsId());

        } else {
            sb.append("dhcp options");
        }

        return sb.toString();
    }

    private void addDhcpConfiguration(Collection<NewDhcpConfiguration> dhcpConfigurations, String configName, List<String> newConfiguration) {
        if (!newConfiguration.isEmpty()) {
            NewDhcpConfiguration dhcpConfiguration = NewDhcpConfiguration.builder()
                .key(configName)
                .values(newConfiguration)
                .build();
            dhcpConfigurations.add(dhcpConfiguration);
        }

    }

}