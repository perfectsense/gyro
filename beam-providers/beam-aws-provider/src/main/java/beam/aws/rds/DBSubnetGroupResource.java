package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbSubnetGroupResponse;
import software.amazon.awssdk.services.rds.model.DbSubnetGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbSubnetGroupsResponse;
import software.amazon.awssdk.services.rds.model.Subnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("db-subnet-group")
public class DBSubnetGroupResource extends RdsTaggableResource {

    private String groupName;
    private String groupArn;
    private String description;
    private List<String> subnetIds;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupArn() {
        return groupArn;
    }

    public void setGroupArn(String groupArn) {
        this.groupArn = groupArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getSubnetIds() {
        if (subnetIds == null || subnetIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sorted = new ArrayList<>(subnetIds);
        Collections.sort(sorted);

        return sorted;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    @Override
    protected String getArn() {
        return groupArn;
    }

    @Override
    public boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getGroupName())) {
            throw new BeamException("group-name is missing, unable to load db subnet group.");
        }

        try {
            DescribeDbSubnetGroupsResponse response = client.describeDBSubnetGroups(
                r -> r.dbSubnetGroupName(getGroupName())
            );

            response.dbSubnetGroups().stream()
                .forEach(g -> {
                    setDescription(g.dbSubnetGroupDescription());
                    setGroupName(g.dbSubnetGroupName());
                    setSubnetIds(g.subnets().stream().map(Subnet::subnetIdentifier).collect(Collectors.toList()));
                    setGroupArn(g.dbSubnetGroupArn());
                }
            );

        } catch (DbSubnetGroupNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        CreateDbSubnetGroupResponse response = client.createDBSubnetGroup(
            r -> r.dbSubnetGroupDescription(getDescription())
                    .dbSubnetGroupName(getGroupName())
                    .subnetIds(getSubnetIds())
        );

        setGroupArn(response.dbSubnetGroup().dbSubnetGroupArn());
    }

    @Override
    public void doUpdate(Resource current, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyDBSubnetGroup(
            r -> r.dbSubnetGroupName(getGroupName())
                    .dbSubnetGroupDescription(getDescription())
                    .subnetIds(getSubnetIds())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBSubnetGroup(
            r -> r.dbSubnetGroupName(getGroupName())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("db subnet group ");
        sb.append(getGroupName());

        return sb.toString();
    }
}
