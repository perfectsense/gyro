package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.MoveAddressToVpcResponse;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * Creates an Elastic IP with the specified Public IP.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::elastic-ip elastic-ip-example
 *         public-ip: 52.20.183.53
 *     end
 */
@ResourceName("elastic-ip")
public class ElasticIpResource extends Ec2TaggableResource<Address> {

    private String allocationId;
    private String publicIp;
    private Boolean isStandardDomain;

    /**
     * Requested public ip for acquirement. See `Elastic IP <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html/>`_.
     */
    @ResourceDiffProperty
    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    /**
     * Allocation id when the requested public ip is acquired.
     */
    @ResourceDiffProperty
    public String getAllocationId() {
        if (allocationId == null) {
            allocationId = "";
        }

        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    @ResourceDiffProperty
    public Boolean getIsStandardDomain() {
        if (isStandardDomain == null) {
            isStandardDomain = false;
        }

        return isStandardDomain;
    }

    public void setIsStandardDomain(Boolean isStandardDomain) {
        this.isStandardDomain = isStandardDomain;
    }

    @Override
    protected String getId() {
        doRefresh();
        return getAllocationId();
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);
        try {
            DescribeAddressesResponse response = getPublicIp() == null
                ? client.describeAddresses() : client.describeAddresses(r -> r.publicIps(getPublicIp()));
            Address address = response.addresses().get(0);
            setAllocationId(address.allocationId());
            setIsStandardDomain(address.domain().equals(DomainType.STANDARD));
            setPublicIp(address.publicIp());

            return true;
        } catch (Ec2Exception eex) {
            if (eex.awsErrorDetails().errorCode().equals("InvalidAllocationID.NotFound")) {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0} not found.", getPublicIp()));
            } else {
                throw eex;
            }
        }
    }

    @Override
    public void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);
        try {
            AllocateAddressResponse response = client.allocateAddress(
                r -> r.address(getPublicIp())
                    .domain(getIsStandardDomain() ? DomainType.STANDARD : DomainType.VPC)
            );
            setAllocationId(response.allocationId());
        } catch (Ec2Exception eex) {
            if (eex.awsErrorDetails().errorCode().equals("InvalidAddress.NotFound")) {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0} Unavailable/Not found.", getPublicIp()));
            } else {
                throw eex;
            }
        }
    }

    @Override
    public void doUpdate(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        if (changedProperties.contains("isStandardDomain")) {
            if (getIsStandardDomain()) {
                MoveAddressToVpcResponse response = client.moveAddressToVpc(r -> r.publicIp("100.24.227.150"));
                setAllocationId(response.allocationId());
                setIsStandardDomain(false);
            } else {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0}, VPC domain to Standard domain not feasible. ", getPublicIp()));
            }
        }
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);
        try {
            client.releaseAddress(r -> r.allocationId(getAllocationId()));
        } catch (Ec2Exception eex) {
            if (eex.awsErrorDetails().errorCode().equals("InvalidAllocationID.NotFound")) {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0} not found.", getPublicIp()));
            } else {
                throw eex;
            }
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getPublicIp() != null && !getPublicIp().isEmpty()) {
            sb.append(getPublicIp());
        } else {
            sb.append("elastic ip");
        }

        return sb.toString();
    }
}
