package gyro.aws.ec2;

import gyro.aws.AwsResource;
import gyro.core.BeamCore;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;

import gyro.core.diff.ResourceOutput;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.MoveAddressToVpcResponse;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;

/**
 * Creates an Elastic IP with the specified Public IP.
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     aws::elastic-ip elastic-ip-example
 *         public-ip: 52.20.183.53
 *         allow-reassociation : true
 *         instance-id : "i-01cde88fd84814ad7"
 *     end
 *
 * .. code-block:: gyro
 *
 *     aws::elastic-ip elastic-ip-example
 *         public-ip: 52.20.183.53
 *         allow-reassociation : true
 *         network-interface-id : "eni-0be88bd89466e3841"
 *     end
 */
@ResourceName("elastic-ip")
public class ElasticIpResource extends Ec2TaggableResource<Address> {

    private String allocationId;
    private String publicIp;
    private Boolean isStandardDomain;
    private String instanceId;
    private String networkInterfaceId;
    private String associationId;
    private Boolean allowReassociation;

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
     * Network Interface id required when the requested public ip is associated with a network interface.
     */
    @ResourceDiffProperty(updatable = true)
    public String getNetworkInterfaceId() {
        return networkInterfaceId;
    }

    public void setNetworkInterfaceId(String networkInterfaceId) {
        this.networkInterfaceId = networkInterfaceId;
    }

    /**
     * Instance id required when the requested public ip is associated with an instance.
     */
    @ResourceDiffProperty(updatable = true)
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Allocation id when the requested public ip is acquired.
     */
    @ResourceDiffProperty
    @ResourceOutput
    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    /**
     * Association id assigned when the requested public ip is associated to an instance or a network interface.
     */
    @ResourceDiffProperty
    public String getAssociationId() {
        return associationId;
    }

    public void setAssociationId(String associationId) {
        this.associationId = associationId;
    }

    /**
     * Allows reassociation of elastic Ip with another resource.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getAllowReassociation() {
        return allowReassociation;
    }

    public void setAllowReassociation(Boolean allowReassociation) {
        this.allowReassociation = allowReassociation;
    }

    @ResourceDiffProperty(updatable = true)
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
        return getAllocationId();
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);
        try {
            DescribeAddressesResponse response = client.describeAddresses(
                r -> r.allocationIds(Collections.singleton(getAllocationId()))
            );
            Address address = response.addresses().get(0);
            setAllocationId(address.allocationId());
            setIsStandardDomain(address.domain().equals(DomainType.STANDARD));
            setPublicIp(address.publicIp());
            setNetworkInterfaceId(address.networkInterfaceId());
            setInstanceId(address.instanceId());
            setAssociationId(address.associationId());

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
            setPublicIp(response.publicIp());
            if (getInstanceId() != null || getNetworkInterfaceId() != null) {
                BeamCore.ui().write("\n@|bold,blue Skipping association of elastic IP"
                    + ", must be updated to associate with a resource|@");
            }
        } catch (Ec2Exception eex) {
            if (eex.awsErrorDetails().errorCode().equals("InvalidAddress.NotFound")) {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0} Unavailable/Not found.", getPublicIp()));
            } else if (eex.awsErrorDetails().errorCode().equals("AddressLimitExceeded")) {
                throw new BeamException("The maximum number of addresses has been reached.");
            }
        }
    }

    @Override
    public void doUpdate(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        if (changedProperties.contains("isStandardDomain")) {
            if (!getIsStandardDomain()) {
                MoveAddressToVpcResponse response = client.moveAddressToVpc(r -> r.publicIp(getPublicIp()));
                setAllocationId(response.allocationId());
                setIsStandardDomain(false);
            } else {
                throw new BeamException(MessageFormat.format("Elastic Ip - {0}, VPC domain to Standard domain not feasible. ", getPublicIp()));
            }
        }

        if (changedProperties.contains("instance-id") || changedProperties.contains("network-interface-id")) {
            if (!getAllowReassociation()) {
                throw new BeamException("Please set the allow re-association to true in order for any associations.");
            }

            if (changedProperties.contains("instance-id")) {
                if (getInstanceId() != null) {
                    AssociateAddressResponse resp = client.associateAddress(r -> r.allocationId(getAllocationId())
                        .instanceId(getInstanceId())
                        .allowReassociation(getAllowReassociation()));
                    setAssociationId(resp.associationId());
                } else {
                    if (!changedProperties.contains("network-interface-id")) {
                        client.disassociateAddress(r -> r.associationId(getAssociationId()));
                    }
                }
            }

            if (changedProperties.contains("network-interface-id")) {
                if (getNetworkInterfaceId() != null) {
                    AssociateAddressResponse resp = client.associateAddress(r -> r.allocationId(getAllocationId())
                        .networkInterfaceId(getNetworkInterfaceId())
                        .allowReassociation(getAllowReassociation()));
                    setAssociationId(resp.associationId());
                } else {
                    if (!changedProperties.contains("instance-id")) {
                        client.disassociateAddress(r -> r.associationId(getAssociationId()));
                    }
                }
            }
        }

        doRefresh();
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeAddressesResponse response = client.describeAddresses(
            r -> r.allocationIds(Collections.singleton(getAllocationId()))
        );
        Address address = response.addresses().get(0);

        try {
            if (address.associationId() != null) {
                client.disassociateAddress(r -> r.associationId(getAssociationId()));
            }
        } catch (Ec2Exception e) {
            throw new BeamException("Non managed associated resource");
        }

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

        sb.append("elastic ip");

        if (getPublicIp() != null && !getPublicIp().isEmpty()) {
            sb.append(" - ").append(getPublicIp());
        }

        return sb.toString();
    }
}
