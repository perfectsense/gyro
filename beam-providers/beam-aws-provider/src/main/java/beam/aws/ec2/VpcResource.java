package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.ClassicLinkDnsSupport;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcClassicLinkDnsSupportResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcClassicLinkResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcAttributeName;
import software.amazon.awssdk.services.ec2.model.VpcClassicLink;

import java.util.Set;

/**
 * Creates a VPC with the specified IPv4 CIDR block.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::vpc example-vpc
 *         cidr-block: 10.0.0.0/16
 *         enable-classic-link: false
 *         enable-dns-hostnames: true
 *         enable-dns-support: true
 *     end
 */
@ResourceName("vpc")
public class VpcResource extends Ec2TaggableResource<Vpc> {

    private String vpcId;
    private String cidrBlock;
    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;
    private String dhcpOptionsId;
    private String instanceTenancy;
    private Boolean enableClassicLink;
    private Boolean enableClassicLinkDnsSupport;

    // Read-only
    private String ownerId;
    private Boolean defaultVpc;

    public String getId() {
        return getVpcId();
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    /**
     * The IPv4 network range for the VPC, in CIDR notation. (Required)
     */
    @ResourceDiffProperty
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    /**
     * Launch instances with public hostnames. Defaults to false. See `DNS Support in your VPC <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-dns.html#vpc-dns-support>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsHostnames() {
        if (enableDnsHostnames == null) {
            enableDnsHostnames = true;
        }

        return enableDnsHostnames;
    }

    public void setEnableDnsHostnames(Boolean enableDnsHostnames) {
        this.enableDnsHostnames = enableDnsHostnames;
    }

    /**
     * Enable Amazon provided DNS server at 169.254.169.253 or base of VPC network range plus two. Default is true. See `DNS Support in your VPC <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-dns.html#vpc-dns-support>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsSupport() {
        if (enableDnsSupport == null) {
            enableDnsSupport = true;
        }

        return enableDnsSupport;
    }

    public void setEnableDnsSupport(Boolean enableDnsSupport) {
        this.enableDnsSupport = enableDnsSupport;
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

    /**
     * Set whether instances are launched on shared hardware (``default``) or dedicated hardware (``dedicated``). See `Dedicated Instances <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/dedicated-instance.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getInstanceTenancy() {
        return instanceTenancy;
    }

    public void setInstanceTenancy(String instanceTenancy) {
        this.instanceTenancy = instanceTenancy;
    }

    public Boolean getDefaultVpc() {
        if (defaultVpc == null) {
            defaultVpc = false;
        }

        return defaultVpc;
    }

    public void setDefaultVpc(Boolean defaultVpc) {
        this.defaultVpc = defaultVpc;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Enable ClassLink to allow communication with EC2-Classic instances. Defaults to false. See `ClassicLink Basics <https://docs.aws.amazon.com/vpc/latest/userguide/vpc-classiclink.html/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableClassicLink() {
        if (enableClassicLink == null) {
            enableClassicLink = false;
        }

        return enableClassicLink;
    }

    public void setEnableClassicLink(Boolean enableClassicLink) {
        this.enableClassicLink = enableClassicLink;
    }

    /**
     * Enable linked EC2-Classic instance hostnames to resolve to private IP address. Defaults to false. See `Enabling ClassicLink DNS Support <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/vpc-classiclink.html?#classiclink-enable-dns-support/>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableClassicLinkDnsSupport() {
        if (enableClassicLinkDnsSupport == null) {
            enableClassicLinkDnsSupport = false;
        }

        return enableClassicLinkDnsSupport;
    }

    public void setEnablelassicLinkDnsSupport(Boolean enableClassicLinkDnsSupport) {
        this.enableClassicLinkDnsSupport = enableClassicLinkDnsSupport;
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getVpcId())) {
            throw new BeamException("vpc-id is missing, unable to load vpc.");
        }

        try {
            DescribeVpcsRequest request = DescribeVpcsRequest.builder()
                .vpcIds(getVpcId())
                .build();

            for (Vpc vpc : client.describeVpcs(request).vpcs()) {
                String vpcId = vpc.vpcId();

                setVpcId(vpcId);
                setCidrBlock(vpc.cidrBlock());
                setInstanceTenancy(vpc.instanceTenancyAsString());
                setDhcpOptionsId(vpc.dhcpOptionsId());
                setOwnerId(vpc.ownerId());
                setDefaultVpc(vpc.isDefault());

                loadSettings(client);

                break;
            }
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateVpcRequest request = CreateVpcRequest.builder()
                .cidrBlock(getCidrBlock())
                .instanceTenancy(getInstanceTenancy())
                .build();

        CreateVpcResponse response = client.createVpc(request);

        Vpc vpc = response.vpc();
        setVpcId(response.vpc().vpcId());
        setOwnerId(vpc.ownerId());
        setInstanceTenancy(vpc.instanceTenancyAsString());

        modifySettings(client);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        modifySettings(client);
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(getVpcId())
                .build();

        client.deleteVpc(request);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String vpcId = getVpcId();

        if (vpcId != null) {
            sb.append(vpcId);

        } else {
            sb.append("vpc");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(cidrBlock);
        }

        return sb.toString();
    }

    private void loadSettings(Ec2Client client) {
        // DNS Settings
        DescribeVpcAttributeRequest request = DescribeVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .attribute(VpcAttributeName.ENABLE_DNS_HOSTNAMES)
                .build();
        setEnableDnsHostnames(client.describeVpcAttribute(request).enableDnsHostnames().value());

        request = DescribeVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .attribute(VpcAttributeName.ENABLE_DNS_SUPPORT)
                .build();
        setEnableDnsSupport(client.describeVpcAttribute(request).enableDnsSupport().value());

        // ClassicLink
        try {
            DescribeVpcClassicLinkResponse clResponse = client.describeVpcClassicLink(r -> r.vpcIds(getVpcId()));
            for (VpcClassicLink classicLink : clResponse.vpcs()) {
                setEnableClassicLink(classicLink.classicLinkEnabled());
                break;
            }

            DescribeVpcClassicLinkDnsSupportResponse cldResponse = client.describeVpcClassicLinkDnsSupport(r -> r.vpcIds(getVpcId()));
            for (ClassicLinkDnsSupport classicLink : cldResponse.vpcs()) {
                setEnableClassicLink(classicLink.classicLinkDnsSupported());
                break;
            }
        } catch (Ec2Exception ex) {
            if (!ex.getLocalizedMessage().contains("not available in this region")) {
                throw ex;
            }
        }
    }

    private void modifySettings(Ec2Client client) {
        // DNS Settings
        if (getEnableDnsHostnames() != null) {
            ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsHostnames(AttributeBooleanValue.builder().value(getEnableDnsHostnames()).build())
                    .build();

            client.modifyVpcAttribute(request);
        }

        if (getEnableDnsSupport() != null) {
            ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsSupport(AttributeBooleanValue.builder().value(getEnableDnsSupport()).build())
                    .build();

            client.modifyVpcAttribute(request);

            request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsHostnames(AttributeBooleanValue.builder().value(getEnableDnsHostnames()).build())
                    .build();

            client.modifyVpcAttribute(request);
        }

        // DCHP Options
        if (getDhcpOptionsId() != null) {
            client.associateDhcpOptions(r -> r.dhcpOptionsId(getDhcpOptionsId()).vpcId(getVpcId()));
        }

        // ClassicLink
        try {
            if (getEnableClassicLink()) {
                client.enableVpcClassicLink(r -> r.vpcId(getVpcId()));
            } else {
                client.disableVpcClassicLink(r -> r.vpcId(getVpcId()));
            }

            if (getEnableClassicLinkDnsSupport()) {
                client.enableVpcClassicLinkDnsSupport(r -> r.vpcId(getVpcId()));
            } else {
                client.disableVpcClassicLinkDnsSupport(r -> r.vpcId(getVpcId()));
            }
        } catch (Ec2Exception ex) {
            if (!ex.getLocalizedMessage().contains("not available in this region")) {
                throw ex;
            }
        }

        // Tenancy
        client.modifyVpcTenancy(r -> r.instanceTenancy(getInstanceTenancy()).vpcId(getVpcId()));
    }

}
