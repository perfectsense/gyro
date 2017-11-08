package beam.aws.config;

import java.lang.reflect.Constructor;
import java.util.*;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;

import beam.diff.ResourceDiffProperty;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.*;
import com.psddev.dari.util.StringUtils;

public class HostedZoneResource extends AWSResource<HostedZone> {

    private String id;
    private String name;
    private Set<HostedZoneRRSetResource> resourceRecordSets;
    private List<VPC> vpcs;
    private List<BeamReference> vpcResources;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass, AWSCredentialsProvider provider) {

        try {
            Constructor<?> constructor = clientClass.getConstructor(AWSCredentialsProvider.class);

            T client = (T) constructor.newInstance(provider);

            if (getRegion() != null) {
                client.setRegion(getRegion());
            }

            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<HostedZoneRRSetResource> getResourceRecordSets() {
        if (resourceRecordSets == null) {
            resourceRecordSets = new HashSet<>();
        }

        return resourceRecordSets;
    }

    public void setResourceRecordSets(Set<HostedZoneRRSetResource> resourceRecordSets) {
        this.resourceRecordSets = resourceRecordSets;
    }

    public List<VPC> getVpcs() {
        if (vpcs == null) {
            vpcs = new ArrayList<>();
        }

        return vpcs;
    }

    public void setVpcs(List<VPC> vpcs) {
        this.vpcs = vpcs;
    }

    @ResourceDiffProperty(updatable = true)
    public List<BeamReference> getVpcResources() {
        if (vpcResources == null) {
            vpcResources = new ArrayList<BeamReference>();
        }

        return vpcResources;
    }

    public void setVpcResources(List<BeamReference> vpcs) {
        this.vpcResources = vpcResources;
    }

    public boolean isPrivate() {
       return getVpcs().size() > 0 || getVpcResources().size() > 0;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getName(), isPrivate() ? "private" : "public");
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, HostedZone zone) {
        String id = zone.getId();
        if (zone.getId().startsWith("/")) {
            id = zone.getId().substring(zone.getId().lastIndexOf("/") + 1);
        }

        setId(id);
        setName(zone.getName());

        // BeamResource record sets.
        AmazonRoute53Client client = createClient(AmazonRoute53Client.class, cloud.getProvider());
        ListResourceRecordSetsRequest lrrsRequest = new ListResourceRecordSetsRequest();

        lrrsRequest.setHostedZoneId(id);
        ListResourceRecordSetsResult lrrsResult;
        do {
            lrrsResult = client.listResourceRecordSets(lrrsRequest);
            for (ResourceRecordSet rrs : lrrsResult.getResourceRecordSets()) {
                String type = rrs.getType();

                if (!"NS".equals(type) &&
                        !"SOA".equals(type) &&
                        isInclude(filter, rrs)) {

                    HostedZoneRRSetResource rrsResource = new HostedZoneRRSetResource();

                    rrsResource.init(cloud, filter, rrs);
                    getResourceRecordSets().add(rrsResource);
                }
            }

            lrrsRequest.setStartRecordIdentifier(lrrsResult.getNextRecordIdentifier());
            lrrsRequest.setStartRecordName(lrrsResult.getNextRecordName());
            lrrsRequest.setStartRecordType(lrrsResult.getNextRecordType());
        } while (lrrsResult.isTruncated());

        if (zone.getConfig().isPrivateZone()) {
            GetHostedZoneRequest hzRequest = new GetHostedZoneRequest();
            hzRequest.setId(getId());

            GetHostedZoneResult hzResult = client.getHostedZone(hzRequest);
            getVpcs().addAll(hzResult.getVPCs());
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getResourceRecordSets());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, HostedZone> current) throws Exception {
        HostedZoneResource currentHz = (HostedZoneResource) current;

        update.update(currentHz.getResourceRecordSets(), getResourceRecordSets());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getResourceRecordSets());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonRoute53Client client = createClient(AmazonRoute53Client.class, cloud.getProvider());
        CreateHostedZoneRequest chzRequest = new CreateHostedZoneRequest();
        String name = getName();

        List<VPC> r53Vpcs = new ArrayList<>();
        for (BeamReference reference : getVpcResources()) {
            VpcResource vpcResource = (VpcResource) reference.resolve();

            VPC vpc = new VPC();
            vpc.setVPCId(vpcResource.getVpcId());
            vpc.setVPCRegion(vpcResource.getRegion().getName());

            r53Vpcs.add(vpc);
        }

        if (r53Vpcs.size() > 0) {
            chzRequest.setVPC(r53Vpcs.remove(0));
        }

        chzRequest.setCallerReference(StringUtils.hex(StringUtils.md5(new Date().toString())).substring(0, 8) + "-private-" + name);
        chzRequest.setName(name);
        setId(client.createHostedZone(chzRequest).getHostedZone().getId());

        for (VPC vpc : r53Vpcs) {
            AssociateVPCWithHostedZoneRequest zoneRequest = new AssociateVPCWithHostedZoneRequest();
            zoneRequest.setVPC(vpc);
            zoneRequest.setHostedZoneId(getId());

            client.associateVPCWithHostedZone(zoneRequest);
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, HostedZone> current, Set<String> changedProperties) {
        AmazonRoute53Client client = createClient(AmazonRoute53Client.class, cloud.getProvider());

        for (BeamReference reference : getVpcResources()) {
            VpcResource vpcResource = (VpcResource) reference.resolve();

            VPC vpc = new VPC();
            vpc.setVPCId(vpcResource.getVpcId());
            vpc.setVPCRegion(vpcResource.getRegion().getName());

            AssociateVPCWithHostedZoneRequest zoneRequest = new AssociateVPCWithHostedZoneRequest();
            zoneRequest.setVPC(vpc);
            zoneRequest.setHostedZoneId(getId());

            try {
                client.associateVPCWithHostedZone(zoneRequest);
            } catch (ConflictingDomainExistsException cdee) {

            }
        }
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AWSCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplayString() {
        return (getVpcResources().size() == 0 ? "" : "private ") + "hosted zone " + getName();
    }
}
