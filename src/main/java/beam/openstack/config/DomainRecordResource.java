package beam.openstack.config;


import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.config.ConfigKey;
import beam.config.ConfigValue;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Job;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;
import org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class DomainRecordResource extends OpenStackResource<RecordDetail> {

    private BeamReference domain;
    private String name;
    private Integer ttl;
    private String type;
    private Value value;
    private String comment;
    private String recordId;
    private int domainId;

    private RecordDetail recordDetail;

    public BeamReference getDomain() {
        return newParentReference(DomainResource.class, domain);
    }

    public void setDomain(BeamReference domain) {
        this.domain = domain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getTTL() {
        return ttl;
    }

    public void setTTL(Integer ttl) {
        this.ttl = ttl;
    }

    @ResourceDiffProperty(updatable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RecordDetail getRecordDetail() {
        return recordDetail;
    }

    public void setRecordDetail(RecordDetail recordDetail) {
        this.recordDetail = recordDetail;
    }

    @ResourceDiffProperty(updatable = true)
    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    @Override
    public List<Object> diffIds() {
        return Arrays.asList((Object) getName(), getValue().resolve());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, RecordDetail recordDetail) {
        setRecordId(recordDetail.getId());
        setName(recordDetail.getName());
        setValue(new StringValue(recordDetail.getData()));
        setTTL(recordDetail.getTTL());
        setType(recordDetail.getType());
        setComment(recordDetail.getComment());

        this.recordDetail = recordDetail;
    }

    @Override
    public void create(OpenStackCloud cloud) {
        CloudDNSApi dnsApi = cloud.createCloudDnsApi();

        value.setType(getType());

        Record createRecord = Record.builder()
                .ttl(getTTL())
                .type(getType())
                .data(value.resolve())
                .name(getName())
                .comment(getComment())
                .build();

        DomainResource domainResource = (DomainResource) getDomain().resolve();
        RecordApi recordApi = dnsApi.getRecordApi(domainResource.getDomainId());

        Job<Set<RecordDetail>> job = recordApi.create(Lists.newArrayList(createRecord));

        try {
            Set<RecordDetail> records = JobPredicates.awaitComplete(dnsApi, job);

            for (RecordDetail recordDetail : records) {
                this.recordDetail = recordDetail;
                break;
            }
        } catch (TimeoutException te) {
            throw new BeamException("Timed out waiting for domain creation.");
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, RecordDetail> current, Set<String> changedProperties) {
        CloudDNSApi dnsApi = cloud.createCloudDnsApi();

        DomainResource domainResource = (DomainResource) getDomain().resolve();
        RecordApi recordApi = dnsApi.getRecordApi(domainResource.getDomainId());

        Record updateRecord = Record.builder()
                .data(value.resolve())
                .ttl(getTTL())
                .type(getType())
                .comment(getComment())
                .build();

        Job<Void> job = recordApi.update(recordDetail.getId(), updateRecord);

        try {
            JobPredicates.awaitComplete(dnsApi, job);
        } catch (TimeoutException te) {
            throw new BeamException("Timed out waiting for domain creation.");
        }
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        CloudDNSApi dnsApi = cloud.createCloudDnsApi();

        RecordApi recordApi = dnsApi.getRecordApi(getDomainId());
        recordApi.delete(getRecordId());
    }

    @Override
    public String toDisplayString() {
        return "DNS record '" + getName() + "' " + getType() + " " + getValue().resolve();
    }

    @ConfigKey("type")
    public static abstract class Value {

        private String type;

        private boolean isPrivate;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isPrivate() {
            return isPrivate;
        }

        public void setPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public abstract String resolve();

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return resolve();
        }
    }

    @ConfigValue("string")
    public static class StringValue extends Value {

        private String string;

        public StringValue(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        @Override
        public String resolve() {
            return getString();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof Value) {
                return ObjectUtils.equals(
                        resolve(),
                        ((Value) other).resolve());

            } else {
                return false;
            }
        }
    }

    @ConfigValue("reference")
    public static class ReferenceValue extends Value {

        private BeamReference reference;

        public ReferenceValue(BeamReference reference) {
            this.reference = reference;
        }

        public BeamReference getReference() {
            return reference;
        }

        public void setReference(BeamReference reference) {
            this.reference = reference;
        }

        @Override
        public String resolve() {
            BeamReference reference = getReference();

            if (reference != null) {
                BeamResource resource = reference.resolve();

                if (resource instanceof ServerResource) {
                    ServerResource serverResource = (ServerResource) resource;

                    if (!isPrivate()) {
                        return serverResource.getPublicIP();
                    } else {
                        return serverResource.getPrivateIP();
                    }
                } else if (resource instanceof LoadBalancerResource) {
                    LoadBalancerResource loadBalancerResource = (LoadBalancerResource) resource;

                    return loadBalancerResource.getVirtualIp4();
                }
            }

            return null;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof ReferenceValue) {
                return ObjectUtils.equals(
                        getReference(),
                        ((ReferenceValue) other).getReference());

            } else if (other instanceof Value) {
                return ObjectUtils.equals(
                        resolve(),
                        ((Value) other).resolve());

            } else {
                return false;
            }
        }
    }

}
