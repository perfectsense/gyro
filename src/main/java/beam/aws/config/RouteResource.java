package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.DeleteRouteRequest;
import com.amazonaws.services.ec2.model.ReplaceRouteRequest;
import com.amazonaws.services.ec2.model.Route;

public class RouteResource extends AWSResource<Route> {

    private String destinationCidrBlock;
    private BeamReference routeTable;
    private BeamReference target;

    public String getDestinationCidrBlock() {
        return destinationCidrBlock;
    }

    public void setDestinationCidrBlock(String destinationCidrBlock) {
        this.destinationCidrBlock = destinationCidrBlock;
    }

    public BeamReference getRouteTable() {
        return newParentReference(RouteTableResource.class, routeTable);
    }

    public void setRouteTableId(BeamReference routeTable) {
        this.routeTable = routeTable;
    }

    @ResourceDiffProperty(updatable = true)
    public BeamReference getTarget() {
        return target;
    }

    public void setTarget(BeamReference target) {
        this.target = target;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getDestinationCidrBlock());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, Route route) {
        setDestinationCidrBlock(route.getDestinationCidrBlock());

        String gatewayId = route.getGatewayId();
        String instanceId = route.getInstanceId();
        String natGatewayId = route.getNatGatewayId();

        if (gatewayId != null) {
            setTarget(newReference(InternetGatewayResource.class, gatewayId));

        } else if (instanceId != null) {
            setTarget(newReference(InstanceResource.class, instanceId));

        } else if (natGatewayId != null) {
            setTarget(newReference(NatGatewayResource.class, natGatewayId));
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateRouteRequest crRequest = new CreateRouteRequest();

        crRequest.setDestinationCidrBlock(getDestinationCidrBlock());
        crRequest.setRouteTableId(getRouteTable().awsId());

        BeamReference target = getTarget();
        Class<? extends BeamResource> targetResourceClass = target.getResourceClass();

        if (InternetGatewayResource.class.isAssignableFrom(targetResourceClass)) {
            crRequest.setGatewayId(target.awsId());

        } else if (InstanceResource.class.isAssignableFrom(targetResourceClass)) {
            crRequest.setInstanceId(target.awsId());

        } else if (NatGatewayResource.class.isAssignableFrom(targetResourceClass)) {
            crRequest.setNatGatewayId(target.awsId());
        }

        client.createRoute(crRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Route> current, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        ReplaceRouteRequest rrRequest = new ReplaceRouteRequest();

        rrRequest.setDestinationCidrBlock(getDestinationCidrBlock());
        rrRequest.setRouteTableId(getRouteTable().awsId());

        BeamReference target = getTarget();
        Class<? extends BeamResource> targetResourceClass = target.getResourceClass();

        if (InternetGatewayResource.class.isAssignableFrom(targetResourceClass)) {
            rrRequest.setGatewayId(target.awsId());

        } else if (InstanceResource.class.isAssignableFrom(targetResourceClass)) {
            rrRequest.setInstanceId(target.awsId());

        } else if (NatGatewayResource.class.isAssignableFrom(targetResourceClass)) {
            rrRequest.setNatGatewayId(target.awsId());
        }

        client.replaceRoute(rrRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteRouteRequest drRequest = new DeleteRouteRequest();

        drRequest.setDestinationCidrBlock(getDestinationCidrBlock());
        drRequest.setRouteTableId(getRouteTable().awsId());
        client.deleteRoute(drRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("route ");
        sb.append(getDestinationCidrBlock());

        BeamReference target = getTarget();
        if (target != null) {
            Class<? extends BeamResource> targetResourceClass = target.getResourceClass();

            if (InternetGatewayResource.class.isAssignableFrom(targetResourceClass)) {
                sb.append(" through gateway ");
                sb.append(target);

            } else if (InstanceResource.class.isAssignableFrom(targetResourceClass)) {
                sb.append(" through instance ");
                sb.append(target);
            }
        }

        return sb.toString();
    }
}
