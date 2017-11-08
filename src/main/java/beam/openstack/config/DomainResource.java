package beam.openstack.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceChange;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.CreateDomain;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Job;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.clouddns.v1.features.DomainApi;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;
import org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class DomainResource extends OpenStackResource<Domain> {

    private String name;
    private String email;
    private Integer domainId;
    private List<DomainRecordResource> records;

    private Domain domain;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public void setDomainId(Integer domainId) {
        this.domainId = domainId;
    }

    public List<DomainRecordResource> getRecords() {
        if (records == null) {
            records = new ArrayList<>();
        }

        return records;
    }

    public void setRecords(List<DomainRecordResource> records) {
        this.records = records;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Domain domain) {
        setName(domain.getName());
        setEmail(domain.getEmail());
        setDomainId(domain.getId());

        CloudDNSApi dnsApi = cloud.createCloudDnsApi();
        RecordApi recordApi = dnsApi.getRecordApi(getDomainId());
        for (RecordDetail recordDetail : recordApi.list().concat()) {
            String type = recordDetail.getType();

            if (!"NS".equals(type) && !"SOA".equals(type) && isInclude(filter, recordDetail)) {
                DomainRecordResource recordResource = new DomainRecordResource();
                recordResource.init(cloud, filter, recordDetail);
                recordResource.setDomainId(getDomainId());

                getRecords().add(recordResource);
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getRecords());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Domain> current) throws Exception {
        DomainResource domainResource = (DomainResource) current;

        update.update(domainResource.getRecords(), getRecords());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getRecords());
    }

    @Override
    public void create(OpenStackCloud cloud) {
        CloudDNSApi dnsApi = cloud.createCloudDnsApi();
        DomainApi domainApi = dnsApi.getDomainApi();

        CreateDomain createDomain = CreateDomain.builder()
                .name(getName())
                .email(getEmail())
                .build();

        Job<Set<Domain>> job = domainApi.create(Lists.newArrayList(createDomain));

        try {
            Set<Domain> domains = JobPredicates.awaitComplete(dnsApi, job);

            Domain domain = domains.iterator().next();
            setDomain(domain);
            setDomainId(domain.getId());
        } catch (TimeoutException te) {
            throw new BeamException("Timed out waiting for domain creation.");
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Domain> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        return "DNS domain " + getName();
    }
}