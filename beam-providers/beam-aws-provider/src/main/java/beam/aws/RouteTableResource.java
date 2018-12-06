package beam.aws;

import beam.core.BeamCredentials;
import beam.core.diff.ResourceName;
import beam.lang.BeamContextKey;
import beam.lang.BeamLiteral;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.RouteTable;

import java.util.Set;

@ResourceName("route-table")
public class RouteTableResource extends TaggableResource<RouteTable> {

    private String routeTableId;
    private String ownerId;
    private String vpcId;

    public String getRouteTableId() {
        return routeTableId;
    }

    public void setRouteTableId(String routeTableId) {
        this.routeTableId = routeTableId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public void refresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeRouteTablesResponse response = client.describeRouteTables(r -> r.filters(
                Filter.builder().name("route-table-id").values(getRouteTableId()).build()
        ));

        for (RouteTable routeTable : response.routeTables()) {
            setVpcId(routeTable.vpcId());
            setOwnerId(routeTable.ownerId());

            return;
        }
    }

    @Override
    protected String getId() {
        return getRouteTableId();
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateRouteTableResponse response = client.createRouteTable(r -> r.vpcId(getVpcId()));

        setRouteTableId(response.routeTable().routeTableId());
        setOwnerId(response.routeTable().ownerId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

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
