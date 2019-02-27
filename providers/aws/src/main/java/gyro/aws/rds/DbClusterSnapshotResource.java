package gyro.aws.rds;

import gyro.core.BeamException;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbClusterSnapshotResponse;
import software.amazon.awssdk.services.rds.model.DbClusterSnapshotNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbClusterSnapshotsResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbClusterStateException;

import java.util.Set;

/**
 * Create a db cluster snapshot.
 *
 * .. code-block:: gyro
 *
 *    aws::db-cluster-snapshot db-cluster-snapshot-example
 *        db-cluster-identifier: $(aws::db-cluster db-cluster-example | db-cluster-identifier)
 *        db-cluster-snapshot-identifier: "db-cluster-snapshot-example"
 *        tags: {
 *            Name: "db-cluster-snapshot-example"
 *        }
 *    end
 */
@ResourceName("db-cluster-snapshot")
public class DbClusterSnapshotResource extends RdsTaggableResource {

    private String dbClusterIdentifier;
    private String dbClusterSnapshotIdentifier;

    /**
     * The identifier of the DB cluster to create a snapshot for. (Required)
     */
    public String getDbClusterIdentifier() {
        return dbClusterIdentifier;
    }

    public void setDbClusterIdentifier(String dbClusterIdentifier) {
        this.dbClusterIdentifier = dbClusterIdentifier;
    }

    /**
     * The unique identifier of the DB cluster snapshot. (Required)
     */
    public String getDbClusterSnapshotIdentifier() {
        return dbClusterSnapshotIdentifier;
    }

    public void setDbClusterSnapshotIdentifier(String dbClusterSnapshotIdentifier) {
        this.dbClusterSnapshotIdentifier = dbClusterSnapshotIdentifier;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getDbClusterSnapshotIdentifier())) {
            throw new BeamException("db-cluster-snapshot-identifier is missing, unable to load db cluster snapshot.");
        }

        try {
            DescribeDbClusterSnapshotsResponse response = client.describeDBClusterSnapshots(
                r -> r.dbClusterSnapshotIdentifier(getDbClusterSnapshotIdentifier())
            );

            response.dbClusterSnapshots().stream()
                .forEach(s -> {
                    setDbClusterIdentifier(s.dbClusterIdentifier());
                    setArn(s.dbClusterSnapshotArn());
                }
            );

        } catch (DbClusterSnapshotNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        try {
            RdsClient client = createClient(RdsClient.class);
            CreateDbClusterSnapshotResponse response = client.createDBClusterSnapshot(
                r -> r.dbClusterIdentifier(getDbClusterIdentifier())
                    .dbClusterSnapshotIdentifier(getDbClusterSnapshotIdentifier())
            );

            setArn(response.dbClusterSnapshot().dbClusterSnapshotArn());
        } catch (InvalidDbClusterStateException ex) {
            throw new BeamException(ex.getLocalizedMessage());
        }
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBClusterSnapshot(
            r -> r.dbClusterSnapshotIdentifier(getDbClusterSnapshotIdentifier())
        );
    }

    @Override
    public String toDisplayString() {
        return "db cluster snapshot " + getDbClusterSnapshotIdentifier();
    }
}
