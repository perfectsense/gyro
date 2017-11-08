package beam.aws.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysResult;
import com.amazonaws.services.ec2.model.NatGateway;

public class SubnetResource extends TaggableEC2Resource<Subnet> {

    private String availabilityZone;
    private String cidrBlock;
    private Set<InstanceResource> instances;
    private Boolean mapPublicIpOnLaunch;
    private RouteTableResource routeTable;
    private String subnetId;
    private BeamReference vpc;
    private Set<NatGatewayResource> natGatewayResources;

    @ResourceDiffProperty
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<InstanceResource> getInstances() {
        if (instances == null) {
            instances = new HashSet<>();
        }
        return instances;
    }

    public void setInstances(Set<InstanceResource> instances) {
        this.instances = instances;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(Boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public RouteTableResource getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(RouteTableResource routeTable) {
        this.routeTable = routeTable;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public BeamReference getVpc() {
        return newParentReference(VpcResource.class, vpc);
    }

    public void setVpc(BeamReference vpc) {
        this.vpc = vpc;
    }

    public Set<NatGatewayResource> getNatGatewayResources() {
        if (natGatewayResources == null) {
            natGatewayResources = new HashSet<>();
        }

        return natGatewayResources;
    }

    public void setNatGatewayResources(Set<NatGatewayResource> natGatewayResources) {
        this.natGatewayResources = natGatewayResources;
    }

    @Override
    public String awsId() {
        return getSubnetId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getAvailabilityZone(), getCidrBlock(), getSubnetId());
    }

    @Override
    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, Subnet subnet) {
        String subnetId = subnet.getSubnetId();

        setAvailabilityZone(subnet.getAvailabilityZone());
        setCidrBlock(subnet.getCidrBlock());
        setMapPublicIpOnLaunch(subnet.getMapPublicIpOnLaunch());
        setSubnetId(subnetId);
        setVpc(newReference(VpcResource.class, subnet.getVpcId()));

        // Instances that aren't part of auto scaling group.
        Map<String, List<Instance>> instancesByLayerName = new HashMap<>();
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DescribeInstancesRequest diRequest = new DescribeInstancesRequest();

        diRequest.setFilters(Arrays.asList(
                new Filter("subnet-id").withValues(subnetId)));

        for (Reservation r : client.
                describeInstances(diRequest).
                getReservations()) {

            for (Instance i : r.getInstances()) {
                String state = i.getState().getName();

                if (InstanceStateName.ShuttingDown.toString().equals(state) ||
                        InstanceStateName.Terminated.toString().equals(state)) {
                    continue;

                } else if (!isInclude(filter, i)) {
                    continue;
                }

                boolean autoScaling = false;
                String layerName = null;

                for (Tag tag : i.getTags()) {
                    String key = tag.getKey();

                    if ("aws:autoscaling:groupName".equals(key)) {
                        autoScaling = true;

                    } else if ("beam.layer".equals(key)) {
                        layerName = tag.getValue();
                    }
                }

                if (!autoScaling) {
                    List<Instance> instances = instancesByLayerName.get(layerName);

                    if (instances == null) {
                        instances = new ArrayList<>();
                        instancesByLayerName.put(layerName, instances);
                    }

                    instances.add(i);
                }
            }
        }

        for (List<Instance> instances : instancesByLayerName.values()) {
            // Sort instances by launch date, then instance id.
            Collections.sort(instances, new Comparator<Instance>() {
                @Override
                public int compare(Instance o1, Instance o2) {
                    if (o1.getLaunchTime().before(o2.getLaunchTime())) {
                        return -1;
                    }

                    if (o1.getLaunchTime().after(o2.getLaunchTime())) {
                        return 1;
                    }

                    if (o1.getLaunchTime().equals(o2.getLaunchTime())) {
                        return o1.getInstanceId().compareTo(o2.getInstanceId());
                    }

                    return 0;
                }
            });

            Integer beamLaunchIndex = 0;
            for (Instance instance : instances) {
                InstanceResource iResource = new InstanceResource();
                iResource.setRegion(getRegion());
                iResource.setBeamLaunchIndex(beamLaunchIndex++);

                iResource.init(cloud, filter, instance);
                getInstances().add(iResource);
            }
        }

        // NAT gateway
        DescribeNatGatewaysRequest dngRequest = new DescribeNatGatewaysRequest();
        dngRequest.setFilter(Arrays.asList(
                new Filter("subnet-id").withValues(getSubnetId())));

        DescribeNatGatewaysResult dngResult = client.describeNatGateways(dngRequest);
        for (NatGateway natGateway : dngResult.getNatGateways()) {
            if ("available".equals(natGateway.getState())) {
                NatGatewayResource natGatewayResource = new NatGatewayResource();
                natGatewayResource.setRegion(getRegion());
                natGatewayResource.setSubnet(natGatewayResource.newReference(this));
                natGatewayResource.init(cloud, filter, natGateway);
                getNatGatewayResources().add(natGatewayResource);
            }
        }

        // Route table.
        DescribeRouteTablesRequest drtRequest = new DescribeRouteTablesRequest();

        drtRequest.setFilters(Arrays.asList(
                new Filter("association.subnet-id").withValues(getSubnetId())));

        for (RouteTable rt : client.
                describeRouteTables(drtRequest).
                getRouteTables()) {

            if (isInclude(filter, rt)) {
                RouteTableResource rtResource = new RouteTableResource();
                rtResource.setRegion(getRegion());

                rtResource.init(cloud, filter, rt);
                setRouteTable(rtResource);
                break;
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getNatGatewayResources());
        create.createOne(getRouteTable());
        create.create(getInstances());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Subnet> current) throws Exception {
        SubnetResource currentSubnet = (SubnetResource) current;

        update.update(currentSubnet.getNatGatewayResources(), getNatGatewayResources());
        update.updateOne(currentSubnet.getRouteTable(), getRouteTable());
        update.update(currentSubnet.getInstances(), getInstances());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getInstances());
        delete.deleteOne(getRouteTable());
        delete.delete(getNatGatewayResources());
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateSubnetRequest csRequest = new CreateSubnetRequest();

        csRequest.setAvailabilityZone(getAvailabilityZone());
        csRequest.setCidrBlock(getCidrBlock());
        csRequest.setVpcId(getVpc().awsId());
        setSubnetId(client.createSubnet(csRequest).getSubnet().getSubnetId());
        modifyAttribute(client);
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<Subnet> current, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        modifyAttribute(client);
    }

    private void modifyAttribute(AmazonEC2Client client) {
        Boolean mapPublicIpOnLaunch = getMapPublicIpOnLaunch();

        if (mapPublicIpOnLaunch != null) {
            ModifySubnetAttributeRequest msaRequest = new ModifySubnetAttributeRequest();

            msaRequest.setSubnetId(getSubnetId());
            msaRequest.setMapPublicIpOnLaunch(mapPublicIpOnLaunch);
            client.modifySubnetAttribute(msaRequest);
        }
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        // Network interfaces may still be detaching, so check and wait
        // before deleting the subnet.
        while (true) {
            DescribeNetworkInterfacesRequest dniRequest = new DescribeNetworkInterfacesRequest();

            dniRequest.setFilters(Arrays.asList(
                    new Filter("subnet-id").withValues(getSubnetId())));

            if (client.
                    describeNetworkInterfaces(dniRequest).
                    getNetworkInterfaces().
                    isEmpty()) {
                break;
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                break;
            }
        }

        DeleteSubnetRequest dsRequest = new DeleteSubnetRequest();

        dsRequest.setSubnetId(getSubnetId());
        client.deleteSubnet(dsRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String subnetId = getSubnetId();

        if (subnetId != null) {
            sb.append(subnetId);

        } else {
            sb.append("subnet");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(getCidrBlock());
        }

        String availabilityZone = getAvailabilityZone();

        if (availabilityZone != null) {
            sb.append(" in ");
            sb.append(availabilityZone);
        }

        return sb.toString();
    }
}
