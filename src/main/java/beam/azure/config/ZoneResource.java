package beam.azure.config;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.BeamRuntime;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.dns.DnsManagementClient;
import com.microsoft.azure.management.dns.RecordSetOperations;
import com.microsoft.azure.management.dns.ZoneOperations;
import com.microsoft.azure.management.dns.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ZoneResource extends AzureResource<Zone> {
    private String id;
    private String name;
    private List<RecordSetResource> records;

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

    public List<RecordSetResource> getRecords() {
        if (records == null) {
            records  = new ArrayList<>();
        }

        return records;
    }

    public void setRecords(List<RecordSetResource> records) {
        this.records = records;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, Zone zone) {
        setId(zone.getId());
        setName(zone.getName());

        DnsManagementClient client = cloud.createDnsManagementClient();
        RecordSetOperations rSOperations = client.getRecordSetsOperations();

        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), "eastus");
        String zoneName =  getName();

        try {
            List<RecordSet> ARecordSet = rSOperations.list(resourceGroup, zoneName, RecordType.valueOf("A"), new RecordSetListParameters()).getRecordSets();
            for (RecordSet recordSet : ARecordSet) {
                if (!isInclude(filter, recordSet)) {
                    continue;
                }

                RecordSetResource recordSetResource = new RecordSetResource();
                recordSetResource.setType("A");
                recordSetResource.init(cloud, filter, recordSet);
                getRecords().add(recordSetResource);
            }

            List<RecordSet> CnameRecordSet = rSOperations.list(resourceGroup, zoneName, RecordType.valueOf("CNAME"), new RecordSetListParameters()).getRecordSets();
            for (RecordSet recordSet : CnameRecordSet) {
                if (!isInclude(filter, recordSet)) {
                    continue;
                }

                RecordSetResource recordSetResource = new RecordSetResource();
                recordSetResource.setType("CNAME");
                recordSetResource.init(cloud, filter, recordSet);
                getRecords().add(recordSetResource);
            }

        } catch (Exception error) {
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getRecords());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, Zone> current) throws Exception {
        ZoneResource zoneResource= (ZoneResource) current;
        update.update(zoneResource.getRecords(), getRecords());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getRecords());
    }

    @Override
    public void create(AzureCloud cloud) {
        DnsManagementClient client = cloud.createDnsManagementClient();
        ZoneOperations zoneOperations = client.getZonesOperations();

        ZoneCreateOrUpdateParameters parameters = new ZoneCreateOrUpdateParameters();
        Zone zone = new Zone();
        zone.setName(getName());
        zone.setLocation(getRegion());

        ZoneProperties properties = new ZoneProperties();
        zone.setProperties(properties);
        parameters.setZone(zone);

        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), "eastus");

        try {
            zoneOperations.createOrUpdate(resourceGroup, getName(), parameters);
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to create or update dns zone: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, Zone> current, Set<String> changedProperties) {

    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AzureCloud cloud) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDisplayString() {
        return ("dns zone " + getName());
    }
}
