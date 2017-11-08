package beam.aws.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceDiffProperty;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.VpcEndpoint;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.CreateVpcEndpointResult;
import com.amazonaws.services.ec2.model.DeleteVpcEndpointsRequest;
import com.amazonaws.services.ec2.model.CreateVpcEndpointRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RouteTableAssociation;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;

public class VpcEndpointResource extends AWSResource<VpcEndpoint> {
    private String name;
    private String service;
    private Set<String> subnetSignatures;
    private String policy;
    private String policyDocument;
    private Map<String, Object> policyDetails;
    private String vpcEndpointId;
    private BeamReference vpc;

    public String getVpcId() {
        VpcResource vpcResource = (VpcResource)getVpc().resolve();
        return vpcResource.getVpcId();
    }

    public String getName() {
        if (name != null) {
            return name;
        } else {
            Map<String, Object> policyDetails = getPolicyDetails();

            try {
                List<Map<String, String>> attributeList = (List<Map<String, String>>) policyDetails.get("Statement");
                return attributeList.get(0).get("Sid");
            } catch (Exception error) {
                throw new BeamException("Invalid policy file format. " + error);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVpcEndpointId() {
        return vpcEndpointId;
    }

    public void setVpcEndpointId(String vpcEndpointId) {
        this.vpcEndpointId = vpcEndpointId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPolicyDocument() {
        return policyDocument;
    }

    public void setPolicyDocument(String policyDocument) {
        this.policyDocument = policyDocument;
    }

    public BeamReference getVpc() {
        return newParentReference(VpcResource.class, vpc);
    }

    public void setVpc(BeamReference vpc) {
        this.vpc = vpc;
    }

    @ResourceDiffProperty(updatable = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPolicyDetails() {
        if (policyDetails != null) {
            return policyDetails;
        }

        try {
            Map<String, Object> policyMap = (Map<String, Object>) ObjectUtils.fromJson(getPolicyDocument());
            if (policyMap.containsKey("Version")) {
                policyMap.remove("Version");
            }

            try {
                List<Map<String, String>> attributeList = (List<Map<String, String>>) policyMap.get("Statement");
                String name = attributeList.get(0).get("Sid");
                if (name.equals("")) {
                    attributeList.get(0).put("Sid", getName());
                }

            } catch (Exception error) {
                throw new BeamException("Invalid policy file format. " + error + getPolicy());
            }

            return policyMap;

        } catch (Exception error) {
            throw Throwables.propagate(error);
        }
    }

    public void setPolicyDetails(Map<String, Object> policyDetails) {
        this.policyDetails = policyDetails;
    }

    @ResourceDiffProperty(updatable = true)
    public Set<String> getSubnetSignatures() {
        if (subnetSignatures == null) {
            subnetSignatures = new NullSet<>();
        }
        return subnetSignatures;
    }

    public void setSubnetSignatures(Set<String> subnetSignatures) {
        this.subnetSignatures = subnetSignatures;
    }

    public List<String> getRouteTables() {
        List<String> routeTables = new ArrayList<>();
        VpcResource vpcResource = (VpcResource)getVpc().resolve();

        for (SubnetResource subnetResource : vpcResource.getSubnets()) {

            String subnetSignature = subnetResource.getAvailabilityZone() + " " + subnetResource.getCidrBlock();
            if (getSubnetSignatures().contains(subnetSignature)) {
                routeTables.add(subnetResource.getRouteTable().getRouteTableId());
            }
        }

        return routeTables;
    }

    public void prepareService(String regionName) {
        if (getService().equals("s3")) {
            setService("com.amazonaws." + regionName + ".s3");
        } else {
            throw new BeamException("Unsupported vpc endpoint service: " + getService());
        }
    }

    public void preparePolicy() throws Exception {
        if (getPolicy() == null) {
            setPolicy("endpoint-s3-default");
        }

        String policyName = getPolicy();
        File policyFile = new File("role-policies/" + policyName + ".json");

        if (!policyFile.exists()) {
            throw new BeamException("Policy file: " + policyName + ".json does not exist.");
        }

        setPolicyDocument(IoUtils.toString(policyFile, Charsets.UTF_8));

        Map<String, Object> policyDetails = getPolicyDetails();

        setPolicyDocument(ObjectUtils.toJson(policyDetails));
        setPolicyDetails(policyDetails);
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, VpcEndpoint vpcEndpoint) {
        setVpcEndpointId(vpcEndpoint.getVpcEndpointId());
        setService(vpcEndpoint.getServiceName());
        setPolicyDocument(vpcEndpoint.getPolicyDocument());
        setPolicyDetails(getPolicyDetails());
        setName(getName());

        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        for (String routeTableId : vpcEndpoint.getRouteTableIds()){
            DescribeRouteTablesRequest drtRequest = new DescribeRouteTablesRequest().withFilters(Arrays.asList(
                    new Filter().withName("association.route-table-id").withValues(routeTableId)));

            for (RouteTable routeTable : client.describeRouteTables(drtRequest).getRouteTables()) {
                for (RouteTableAssociation association : routeTable.getAssociations()) {

                    DescribeSubnetsRequest dsRequest = new DescribeSubnetsRequest().withFilters(Arrays.asList(
                            new Filter().withName("subnet-id").withValues(association.getSubnetId())));

                    Subnet subnet = client.describeSubnets(dsRequest).getSubnets().get(0);
                    getSubnetSignatures().add(subnet.getAvailabilityZone() + " " + subnet.getCidrBlock());
                }
            }
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        CreateVpcEndpointRequest cveRequest = new CreateVpcEndpointRequest();

        cveRequest.setVpcId(getVpcId());
        cveRequest.setServiceName(getService());
        cveRequest.setPolicyDocument(getPolicyDocument());
        cveRequest.setRouteTableIds(getRouteTables());

        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        client.setRegion(getRegion());

        CreateVpcEndpointResult cveResult = client.createVpcEndpoint(cveRequest);

        VpcEndpoint vpcEndpoint = cveResult.getVpcEndpoint();

        while (!vpcEndpoint.getState().equals("available")) {
            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                return;
            }
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, VpcEndpoint> current, Set<String> changedProperties) {
        delete(cloud);
        create(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        DeleteVpcEndpointsRequest deleteRequest = new DeleteVpcEndpointsRequest();
        deleteRequest.setVpcEndpointIds(Arrays.asList(getVpcEndpointId()));

        client.deleteVpcEndpoints(deleteRequest);
    }

    @Override
    public String toDisplayString() {
        return "vpcEndpoint " + getName();
    }
}
