package beam.aws.resources;

import beam.aws.AwsCloud;
import beam.core.BeamResource;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.DeleteRouteRequest;
import com.amazonaws.services.ec2.model.ReplaceRouteRequest;
import com.amazonaws.services.ec2.model.Route;

import java.util.Set;

public class RouteResource extends AwsResource {

    private String destinationCidrBlock;

    public String getDestinationCidrBlock() {
        return destinationCidrBlock;
    }

    public void setDestinationCidrBlock(String destinationCidrBlock) {
        this.destinationCidrBlock = destinationCidrBlock;
    }

    @Override
    public void refresh(AwsCloud cloud) {

    }

    public void init(AwsCloud cloud, Route route) {
        setDestinationCidrBlock(route.getDestinationCidrBlock());
    }

    @Override
    public void create(AwsCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateRouteRequest crRequest = new CreateRouteRequest();

        crRequest.setDestinationCidrBlock(getDestinationCidrBlock());
        client.createRoute(crRequest);
    }

    @Override
    public void update(AwsCloud cloud, BeamResource<AwsCloud> current, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        ReplaceRouteRequest rrRequest = new ReplaceRouteRequest();

        rrRequest.setDestinationCidrBlock(getDestinationCidrBlock());

        client.replaceRoute(rrRequest);
    }

    @Override
    public void delete(AwsCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteRouteRequest drRequest = new DeleteRouteRequest();

        drRequest.setDestinationCidrBlock(getDestinationCidrBlock());
        client.deleteRoute(drRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("route ");
        sb.append(getDestinationCidrBlock());

        return sb.toString();
    }
}
