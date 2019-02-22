package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbSnapshotResponse;
import software.amazon.awssdk.services.rds.model.DbSnapshotNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbSnapshotsResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbInstanceStateException;

import java.util.Set;

/**
 * Create a db snapshot.
 *
 * .. code-block:: beam
 *
 *    aws::db-snapshot db-snapshot-example
 *        db-instance-identifier: $(aws::db-instance db-instance-example | db-instance-identifier)
 *        db-snapshot-identifier: "db-snapshot-example"
 *        tags: {
 *            Name: "db-snapshot-example"
 *        }
 *    end
 */
@ResourceName("db-snapshot")
public class DbSnapShotResource extends RdsTaggableResource {

    private String dbInstanceIdentifier;
    private String dbSnapshotIdentifier;
    private String engineVersion;
    private String optionGroupName;

    public String getDbInstanceIdentifier() {
        return dbInstanceIdentifier;
    }

    public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
        this.dbInstanceIdentifier = dbInstanceIdentifier;
    }

    public String getDbSnapshotIdentifier() {
        return dbSnapshotIdentifier;
    }

    public void setDbSnapshotIdentifier(String dbSnapshotIdentifier) {
        this.dbSnapshotIdentifier = dbSnapshotIdentifier;
    }

    @ResourceDiffProperty(updatable = true)
    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    @ResourceDiffProperty(updatable = true)
    public String getOptionGroupName() {
        return optionGroupName;
    }

    public void setOptionGroupName(String optionGroupName) {
        this.optionGroupName = optionGroupName;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getDbSnapshotIdentifier())) {
            throw new BeamException("db-snapshot-identifier is missing, unable to load db snapshot.");
        }

        try {
            DescribeDbSnapshotsResponse response = client.describeDBSnapshots(
                r -> r.dbSnapshotIdentifier(getDbSnapshotIdentifier())
            );

            response.dbSnapshots().stream()
                .forEach(s -> {
                    setDbInstanceIdentifier(s.dbInstanceIdentifier());
                    setEngineVersion(s.engineVersion());
                    setOptionGroupName(s.optionGroupName());
                    setArn(s.dbSnapshotArn());
                }
            );

        } catch (DbSnapshotNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        try {
            RdsClient client = createClient(RdsClient.class);
            CreateDbSnapshotResponse response = client.createDBSnapshot(
                r -> r.dbInstanceIdentifier(getDbInstanceIdentifier())
                    .dbSnapshotIdentifier(getDbSnapshotIdentifier())
            );

            setArn(response.dbSnapshot().dbSnapshotArn());
        } catch (InvalidDbInstanceStateException ex) {
            throw new BeamException(ex.getLocalizedMessage());
        }
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyDBSnapshot(
            r -> r.dbSnapshotIdentifier(getDbSnapshotIdentifier())
                    .engineVersion(getEngineVersion())
                    .optionGroupName(getOptionGroupName())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBSnapshot(
            r -> r.dbSnapshotIdentifier(getDbSnapshotIdentifier())
        );
    }

    @Override
    public String toDisplayString() {
        return "db snapshot " + getDbSnapshotIdentifier();
    }
}
