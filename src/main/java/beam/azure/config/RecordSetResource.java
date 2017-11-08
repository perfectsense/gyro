package beam.azure.config;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.NullSet;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.dns.DnsManagementClient;
import com.microsoft.azure.management.dns.RecordSetOperations;
import com.microsoft.azure.management.dns.models.*;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import java.util.*;

public class RecordSetResource extends AzureResource<RecordSet> {
    private BeamReference zone;
    private String name;
    private Integer ttl;
    private String type;
    private Set<Value> values;
    private String id;
    private Map<String, String> tags;

    public BeamReference getZone() {
        return newParentReference(ZoneResource.class, zone);
    }

    public void setZone(BeamReference zone) {
        this.zone = zone;
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

    @ResourceDiffProperty(updatable = true)
    public Set<Value> getValues() {
        if (values == null) {
            values = new NullSet<>();
        }
        return values;
    }

    public void setValues(Set<Value> values) {
        this.values = values;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public List<Object> diffIds() {
        return Arrays.asList(getZone(), getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, RecordSet recordSet) {
        setId(recordSet.getId());
        setName(recordSet.getName());
        setTags(recordSet.getTags());

        RecordSetProperties properties = recordSet.getProperties();
        setTTL((int) properties.getTtl());

        if ("A".equals(getType())) {
            ArrayList<ARecord> aRecords = properties.getARecords();
            for (ARecord aRecord : aRecords) {
                getValues().add(new StringValue(aRecord.getIpv4Address()));
            }
        } else if ("CNAME".equals(getType())) {
            CnameRecord cnameRecord = properties.getCnameRecord();
            getValues().add(new StringValue(cnameRecord.getCname()));
        }
    }

    @Override
    public void create(AzureCloud cloud) {
        DnsManagementClient client = cloud.createDnsManagementClient();
        RecordSetOperations rSOperations = client.getRecordSetsOperations();

        RecordSetCreateOrUpdateParameters parameters = new RecordSetCreateOrUpdateParameters();
        RecordSet recordSet = toRecordSet();
        parameters.setRecordSet(recordSet);

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        recordSet.setTags(tags);

        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), "eastus");
        String zoneName =  ((ZoneResource)getZone().resolve()).getName();

        try {
            rSOperations.createOrUpdate(resourceGroup, zoneName, getName(), RecordType.valueOf(getType()), parameters);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update dns record: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, RecordSet> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {
        DnsManagementClient client = cloud.createDnsManagementClient();
        RecordSetOperations rSOperations = client.getRecordSetsOperations();

        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), "eastus");
        String zoneName =  ((ZoneResource)getZone().resolve()).getName();

        try {
            rSOperations.delete(resourceGroup, zoneName, getName(), RecordType.valueOf(getType()), new RecordSetDeleteParameters());
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete dns record: " + getName());
        }
    }

    @Override
    public String toDisplayString() {
        return "dns record '" + getName() + "." + BeamRuntime.getCurrentRuntime().getSubDomain() + "' " + getType();
    }

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
            String value = resolve();

            if (value == null && this instanceof ReferenceValue) {
                ReferenceValue referenceValue = (ReferenceValue) this;
                value = referenceValue.getReference().toString();
            }

            return value;
        }
    }

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

                if (resource instanceof VirtualMachineResource) {
                    VirtualMachineResource virtualMachineResource = (VirtualMachineResource) resource;

                    if (!isPrivate()) {
                        return virtualMachineResource.getPublicIpAddress();
                    } else {
                        return virtualMachineResource.getPrivateIpAddress();
                    }
                } else if (resource instanceof LoadBalancerResource) {
                    LoadBalancerResource loadBalancerResource = (LoadBalancerResource) resource;
                    return loadBalancerResource.getPublicIp();
                } else if (resource instanceof ApplicationGatewayResource) {
                    ApplicationGatewayResource applicationGatewayResource = (ApplicationGatewayResource) resource;
                    return applicationGatewayResource.getPublicIp();
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

    private RecordSet toRecordSet() {
        RecordSet recordSet = new RecordSet();
        RecordSetProperties properties = new RecordSetProperties();
        properties.setTtl(getTTL());
        recordSet.setProperties(properties);
        recordSet.setName(getName());
        recordSet.setLocation(getRegion());

        if ("A".equals(getType())) {
            ArrayList<ARecord> aRecords = new ArrayList<>();
            properties.setARecords(aRecords);

            for (Value value : getValues()) {
                ARecord aRecord = new ARecord();
                String valueString = value.resolve();
                if (valueString != null) {
                    aRecord.setIpv4Address(valueString);
                    aRecords.add(aRecord);
                }
            }
        } else if ("CNAME".equals(getType())) {
            CnameRecord cnameRecord = new CnameRecord();
            properties.setCnameRecord(cnameRecord);

            for (Value value : getValues()) {
                String valueString = value.resolve();
                if (valueString != null) {
                    cnameRecord.setCname(valueString);
                }
            }
        }

        return recordSet;
    }
}
