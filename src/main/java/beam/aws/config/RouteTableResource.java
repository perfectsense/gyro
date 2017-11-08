package beam.aws.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AssociateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DisassociateRouteTableRequest;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;

public class RouteTableResource extends TaggableEC2Resource<RouteTable> {

    private Set<RouteResource> routes;
    private String routeTableId;
    private Set<BeamReference> subnets;
    private BeamReference vpc;

    public Set<RouteResource> getRoutes() {
        if (routes == null) {
            routes = new HashSet<>();
        }
        return routes;
    }

    public void setRoutes(Set<RouteResource> routes) {
        this.routes = routes;
    }

    public String getRouteTableId() {
        return routeTableId;
    }

    public void setRouteTableId(String routeTableId) {
        this.routeTableId = routeTableId;
    }

    /**
     * @return Never {@code null}.
     */
    @ResourceDiffProperty
    public Set<BeamReference> getSubnets() {
        SubnetResource parentSubnet = findParent(SubnetResource.class);

        if (parentSubnet != null) {
            return Collections.singleton(newReference(parentSubnet));

        } else {
            if (subnets == null) {
                subnets = new NullSet<>();
            }

            return subnets;
        }
    }

    public void setSubnets(Set<BeamReference> subnets) {
        this.subnets = subnets;
    }

    public BeamReference getVpc() {
        return newParentReference(VpcResource.class, vpc);
    }

    public void setVpc(BeamReference vpc) {
        this.vpc = vpc;
    }

    @Override
    public String awsId() {
        return getRouteTableId();
    }

    @Override
    public List<String> diffIds() {
        SubnetResource parentSubnet = findParent(SubnetResource.class);

        if (parentSubnet != null) {
            return Arrays.asList(parentSubnet.getSubnetId());

        } else {
            return Arrays.asList(getRouteTableId());
        }
    }

    @Override
    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, RouteTable routeTable) {
        setRouteTableId(routeTable.getRouteTableId());
        setVpc(newReference(VpcResource.class, routeTable.getVpcId()));

        for (Route route : routeTable.getRoutes()) {
            if (!"local".equals(route.getGatewayId()) &&
                    isInclude(filter, route) && route.getDestinationPrefixListId() == null) {

                RouteResource routeResource = new RouteResource();
                routeResource.setRegion(getRegion());

                routeResource.init(cloud, filter, route);
                getRoutes().add(routeResource);
            }
        }

        for (RouteTableAssociation rta : routeTable.getAssociations()) {
            getSubnets().add(newReference(SubnetResource.class, rta.getSubnetId()));
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getRoutes());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, RouteTable> current) throws Exception {
        RouteTableResource currentRt = (RouteTableResource) current;

        update.update(currentRt.getRoutes(), getRoutes());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getRoutes());
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        // Create the route table.
        CreateRouteTableRequest crtRequest = new CreateRouteTableRequest();

        crtRequest.setVpcId(getVpc().awsId());
        setRouteTableId(client.createRouteTable(crtRequest).getRouteTable().getRouteTableId());

        // Associate the route table with subnets.
        String routeTableId = getRouteTableId();

        for (BeamReference subnet : getSubnets()) {
            AssociateRouteTableRequest artRequest = new AssociateRouteTableRequest();

            artRequest.setRouteTableId(routeTableId);
            artRequest.setSubnetId(subnet.awsId());
            client.associateRouteTable(artRequest);
        }
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<RouteTable> current, Set<String> changedProperties) {
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        String routeTableId = getRouteTableId();

        // Find all subnets associated with the route table and disassociate.
        DescribeRouteTablesRequest dertRequest = new DescribeRouteTablesRequest();

        dertRequest.setRouteTableIds(Arrays.asList(routeTableId));

        for (RouteTable rt : client.
                describeRouteTables(dertRequest).
                getRouteTables()) {

            DisassociateRouteTableRequest dirtRequest = new DisassociateRouteTableRequest();

            for (RouteTableAssociation rta : rt.getAssociations()) {
                dirtRequest.setAssociationId(rta.getRouteTableAssociationId());
                client.disassociateRouteTable(dirtRequest);
            }
        }

        // Delete the route table.
        DeleteRouteTableRequest drtRequest = new DeleteRouteTableRequest();

        drtRequest.setRouteTableId(routeTableId);
        client.deleteRouteTable(drtRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String routeTableId = getRouteTableId();

        if (routeTableId != null) {
            sb.append(routeTableId);

        } else {
            sb.append("route table");
        }

        return sb.toString();
    }
}
