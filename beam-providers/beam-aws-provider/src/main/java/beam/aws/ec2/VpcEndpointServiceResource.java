package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AllowedPrincipal;
import software.amazon.awssdk.services.ec2.model.CreateVpcEndpointServiceConfigurationResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointServiceConfigurationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointConnectionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointServicePermissionsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ServiceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ResourceName("vpc-endpoint-service")
public class VpcEndpointServiceResource extends AwsResource {

    private Boolean acceptanceRequired;
    private List<String> networkLoadBalancerArns;
    private List<String> principals;
    private String serviceId;
    private String endpointId;

    public Boolean getAcceptanceRequired() {
        return acceptanceRequired;
    }

    public void setAcceptanceRequired(Boolean acceptanceRequired) {
        this.acceptanceRequired = acceptanceRequired;
    }

    public List<String> getNetworkLoadBalancerArns() {
        if (networkLoadBalancerArns == null) {
            networkLoadBalancerArns = new ArrayList<>();
        }

        return networkLoadBalancerArns;
    }

    public void setNetworkLoadBalancerArns(List<String> networkLoadBalancerArns) {
        this.networkLoadBalancerArns = networkLoadBalancerArns;
    }

    public List<String> getPrincipals() {
        if (principals == null) {
            principals = new ArrayList<>();
        }

        return principals;
    }

    public void setPrincipals(List<String> principals) {
        this.principals = principals;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public boolean refresh() {
        try {
            Ec2Client client = createClient(Ec2Client.class);
            DescribeVpcEndpointServiceConfigurationsResponse configurationResponse =
                    client.describeVpcEndpointServiceConfigurations(r -> r.serviceIds(getServiceId()));

            ServiceConfiguration config = configurationResponse.serviceConfigurations().get(0);
            setAcceptanceRequired(config.acceptanceRequired());
            setNetworkLoadBalancerArns(config.networkLoadBalancerArns());
            setServiceId(config.serviceId());

            DescribeVpcEndpointServicePermissionsResponse permissionsResponse =
                    client.describeVpcEndpointServicePermissions(r -> r.serviceId(getServiceId()));

            if (permissionsResponse != null) {
                getPrincipals().clear();
                for (AllowedPrincipal ap : permissionsResponse.allowedPrincipals()) {
                    getPrincipals().add(ap.principal());
                }
            }

            Filter filter = Filter.builder().name("vpc-endpoint-state")
                    .values("pendingAcceptance").build();
            DescribeVpcEndpointConnectionsResponse connectionsResponse =
                    client.describeVpcEndpointConnections(r -> r.filters(filter));
            if (connectionsResponse != null) {
            //accept or reject connections - neccessary?
            }

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public void create() {
        Ec2Client client = createClient(Ec2Client.class);
        CreateVpcEndpointServiceConfigurationResponse createResponse =
                client.createVpcEndpointServiceConfiguration(r -> r.acceptanceRequired(getAcceptanceRequired())
                                                            .networkLoadBalancerArns(getNetworkLoadBalancerArns()));

        setServiceId(createResponse.serviceConfiguration().serviceId());

        client.modifyVpcEndpointServicePermissions(r -> r.addAllowedPrincipals(getPrincipals())
                                                        .serviceId(getServiceId()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        VpcEndpointServiceResource currentResource = (VpcEndpointServiceResource) current;

        List<String> associateLoadBalancers = new ArrayList<>(getNetworkLoadBalancerArns());
        associateLoadBalancers.removeAll(currentResource.getNetworkLoadBalancerArns());

        List<String> dissociateLoadBalancers = new ArrayList<>(currentResource.getNetworkLoadBalancerArns());
        dissociateLoadBalancers.removeAll(getNetworkLoadBalancerArns());

        if (!associateLoadBalancers.isEmpty()) {
            client.modifyVpcEndpointServiceConfiguration(r -> r.addNetworkLoadBalancerArns(associateLoadBalancers)
                    .serviceId(getServiceId())
                    .acceptanceRequired(getAcceptanceRequired()));
        }

        if (!dissociateLoadBalancers.isEmpty()) {
            client.modifyVpcEndpointServiceConfiguration(r -> r.removeNetworkLoadBalancerArns(dissociateLoadBalancers)
                    .serviceId(getServiceId())
                    .acceptanceRequired(getAcceptanceRequired()));
        }

        List<String> associatePrincipals = new ArrayList<>(getPrincipals());
        associatePrincipals.removeAll(currentResource.getPrincipals());

        List<String> dissociatePrincipals = new ArrayList<>(currentResource.getPrincipals());
        dissociatePrincipals.removeAll(getPrincipals());

        if (!associatePrincipals.isEmpty()) {
            client.modifyVpcEndpointServicePermissions(r -> r.addAllowedPrincipals(associatePrincipals)
                    .serviceId(getServiceId()));
        }

        if (!dissociatePrincipals.isEmpty()) {
            client.modifyVpcEndpointServicePermissions(r -> r.removeAllowedPrincipals(dissociatePrincipals)
                    .serviceId(getServiceId()));
        }
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);
        client.deleteVpcEndpointServiceConfigurations(r -> r.serviceIds(getServiceId()));
    }

    @Override
    public String toDisplayString() {
        return "vpc endpoint service ";
    }
}
