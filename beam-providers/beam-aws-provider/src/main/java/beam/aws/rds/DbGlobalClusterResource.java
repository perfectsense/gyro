package beam.aws.rds;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeGlobalClustersResponse;
import software.amazon.awssdk.services.rds.model.GlobalClusterNotFoundException;

import java.util.Set;

/**
 * Create a db instance.
 *
 * .. code-block:: beam
 *
 *    aws::db-global-cluster db-global-cluster-example
 *        global-cluster-identifier: "aurora-global-cluster"
 *        engine: "aurora"
 *    end
 */
@ResourceName("db-global-cluster")
public class DbGlobalClusterResource extends AwsResource {

    private String databaseName;
    private Boolean deletionProtection;
    private String engine;
    private String engineVersion;
    private String globalClusterIdentifier;
    private String sourceDbClusterIdentifier;
    private Boolean storageEncrypted;

    /**
     * The name for your database of up to 64 alpha-numeric characters. If you do not provide a name, Amazon Aurora will not create a database in the global database cluster you are creating.
     */
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * The deletion protection setting for the new global database. The global database can't be deleted when this value is set to true.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDeletionProtection() {
        return deletionProtection;
    }

    public void setDeletionProtection(Boolean deletionProtection) {
        this.deletionProtection = deletionProtection;
    }

    /**
     * Provides the name of the database engine to be used for this DB cluster.
     */
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * The engine version of the Aurora global database.
     */
    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    /**
     * The cluster identifier of the global database cluster. (Required)
     */
    public String getGlobalClusterIdentifier() {
        return globalClusterIdentifier;
    }

    public void setGlobalClusterIdentifier(String globalClusterIdentifier) {
        this.globalClusterIdentifier = globalClusterIdentifier;
    }

    /**
     * The Amazon Resource Name (ARN) to use as the primary cluster of the global database.
     */
    public String getSourceDbClusterIdentifier() {
        return sourceDbClusterIdentifier;
    }

    public void setSourceDbClusterIdentifier(String sourceDbClusterIdentifier) {
        this.sourceDbClusterIdentifier = sourceDbClusterIdentifier;
    }

    /**
     * The storage encryption setting for the new global database cluster.
     */
    public Boolean getStorageEncrypted() {
        return storageEncrypted;
    }

    public void setStorageEncrypted(Boolean storageEncrypted) {
        this.storageEncrypted = storageEncrypted;
    }

    @Override
    public boolean refresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getGlobalClusterIdentifier())) {
            throw new BeamException("global-cluster-identifier is missing, unable to load db global cluster.");
        }

        try {
            DescribeGlobalClustersResponse response = client.describeGlobalClusters(
                r -> r.globalClusterIdentifier(getGlobalClusterIdentifier())
            );

            response.globalClusters().stream()
                .forEach(c -> {
                        setDatabaseName(c.databaseName());
                        setDeletionProtection(c.deletionProtection());
                        setEngine(c.engine());

                        String version = c.engineVersion();
                        if (getEngineVersion() != null) {
                            version = version.substring(0, getEngineVersion().length());
                        }

                        setEngineVersion(version);
                        setStorageEncrypted(c.storageEncrypted());
                    }
                );

        } catch (GlobalClusterNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void create() {
        RdsClient client = createClient(RdsClient.class);
        client.createGlobalCluster(
            r -> r.databaseName(getDatabaseName())
                    .deletionProtection(getDeletionProtection())
                    .engine(getEngine())
                    .engineVersion(getEngineVersion())
                    .globalClusterIdentifier(getGlobalClusterIdentifier())
                    .sourceDBClusterIdentifier(getSourceDbClusterIdentifier())
                    .storageEncrypted(getStorageEncrypted())
        );
    }

    @Override
    public void update(Resource config, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        DbGlobalClusterResource current = (DbGlobalClusterResource) config;
        // The modify global cluster api currently return a 500
        client.modifyGlobalCluster(
            r -> r.deletionProtection(getDeletionProtection())
                    .globalClusterIdentifier(current.getGlobalClusterIdentifier())
                    .newGlobalClusterIdentifier(getGlobalClusterIdentifier())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteGlobalCluster(
            r -> r.globalClusterIdentifier(getGlobalClusterIdentifier())
        );
    }

    @Override
    public String toDisplayString() {
        return "db global cluster " + getGlobalClusterIdentifier();
    }
}
