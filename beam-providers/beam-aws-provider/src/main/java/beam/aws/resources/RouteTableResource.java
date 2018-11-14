package beam.aws.resources;

import beam.aws.AwsCloud;
import beam.core.BeamException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DisassociateRouteTableRequest;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import com.psddev.dari.util.ObjectUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RouteTableResource extends TaggableResource<RouteTable> {

    private String routeTableId;
    private Set<RouteResource> routes;
    private String vpcId;

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

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public String getId() {
        return getRouteTableId();
    }

    @Override
    public void refresh(AwsCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        if (ObjectUtils.isBlank(getRouteTableId())) {
            throw new BeamException("route-table-id is missing, unable to load route table.");
        }

        DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();
        request.withRouteTableIds(getRouteTableId());

        for (RouteTable routeTable : client.describeRouteTables(request).getRouteTables()) {
            doInit(cloud, routeTable);
            break;
        }
    }

    @Override
    protected void doInit(AwsCloud cloud, RouteTable routeTable) {
        setRouteTableId(routeTable.getRouteTableId());
        setVpcId(routeTable.getVpcId());
    }

    @Override
    protected void doCreate(AwsCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        // Create the route table.
        CreateRouteTableRequest crtRequest = new CreateRouteTableRequest();

        crtRequest.setVpcId(getVpcId());
        setRouteTableId(client.createRouteTable(crtRequest).getRouteTable().getRouteTableId());

        // Associate the route table with subnets.
        String routeTableId = getRouteTableId();
    }

    @Override
    protected void doUpdate(AwsCloud cloud, AwsResource current, Set<String> changedProperties) {
    }

    @Override
    public void delete(AwsCloud cloud) {
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
