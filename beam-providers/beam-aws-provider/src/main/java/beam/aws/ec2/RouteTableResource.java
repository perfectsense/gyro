package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.core.diff.ResourceOutput;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.RouteTableAssociation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Creates a VPC route table.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *     aws::route-table route-table-example
 *         vpc-id: $(aws::vpc vpc-example | vpc-id)
 *
 *         tags:
 *             Name: route-table-example
 *         end
 *     end
 */
@ResourceName("route-table")
public class RouteTableResource extends Ec2TaggableResource<RouteTable> {

    private String vpcId;
    private List<String> subnetIds;
    private String routeTableId;
    private String ownerId;

    /**
     * The id of the VPC to create a route table for.
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    /**
     * Subnet IDs to associate with this route table.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getSubnetIds() {
        if (subnetIds == null) {
            subnetIds = new ArrayList<>();
        }

        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    @ResourceOutput
    public String getRouteTableId() {
        return routeTableId;
    }

    public void setRouteTableId(String routeTableId) {
        this.routeTableId = routeTableId;
    }

    @ResourceOutput
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    protected String getId() {
        return getRouteTableId();
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeRouteTablesResponse response = client.describeRouteTables(r -> r.filters(
                Filter.builder().name("route-table-id").values(getRouteTableId()).build()
        ));

        for (RouteTable routeTable : response.routeTables()) {
            setVpcId(routeTable.vpcId());
            setOwnerId(routeTable.ownerId());

            getSubnetIds().clear();
            for (RouteTableAssociation rta : routeTable.associations()) {
                if (!rta.main()) {
                    getSubnetIds().add(rta.subnetId());
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateRouteTableResponse response = client.createRouteTable(r -> r.vpcId(getVpcId()));

        setRouteTableId(response.routeTable().routeTableId());
        setOwnerId(response.routeTable().ownerId());

        for (String subnetId : getSubnetIds()) {
            client.associateRouteTable(r -> r.routeTableId(getRouteTableId()).subnetId(subnetId));
        }
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        RouteTableResource currentResource = (RouteTableResource) current;

        List<String> additions = new ArrayList<>(getSubnetIds());
        additions.removeAll(currentResource.getSubnetIds());

        List<String> subtractions = new ArrayList<>(currentResource.getSubnetIds());
        subtractions.removeAll(getSubnetIds());

        for (String subnetId : additions) {
            client.associateRouteTable(r -> r.routeTableId(getRouteTableId()).subnetId(subnetId));
        }

        for (String subnetId : subtractions) {
            DescribeRouteTablesResponse response = client.describeRouteTables(r -> r.filters(
                Filter.builder().name("route-table-id").values(getRouteTableId()).build()
            ));

            for (RouteTable routeTable : response.routeTables()) {
                for (RouteTableAssociation rta : routeTable.associations()) {
                    if (!rta.main() && rta.subnetId().equals(subnetId)) {

                        client.disassociateRouteTable(r -> r.associationId(rta.routeTableAssociationId()));
                    }
                }
            }

        }
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeRouteTablesResponse response = client.describeRouteTables(r -> r.filters(
            Filter.builder().name("route-table-id").values(getRouteTableId()).build()
        ));

        for (RouteTable routeTable : response.routeTables()) {
            for (RouteTableAssociation rta : routeTable.associations()) {
                client.disassociateRouteTable(r -> r.associationId(rta.routeTableAssociationId()));
            }
        }

        client.deleteRouteTable(r -> r.routeTableId(getRouteTableId()));
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
