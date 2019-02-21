package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateGlobalClusterResponse;
import software.amazon.awssdk.services.rds.model.DescribeGlobalClustersResponse;
import software.amazon.awssdk.services.rds.model.GlobalClusterNotFoundException;

import java.util.Set;

@ResourceName("db-global-cluster")
public class DbGlobalClusterResource extends RdsTaggableResource {

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
    @ResourceDiffProperty(updatable = true)
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
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getGlobalClusterIdentifier())) {
            throw new BeamException("global-cluster-identifier is missing, unable to load db cluster snapshot.");
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
                        setArn(c.globalClusterArn());
                    }
                );

        } catch (GlobalClusterNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        CreateGlobalClusterResponse response = client.createGlobalCluster(
            r -> r.databaseName(getDatabaseName())
                    .deletionProtection(getDeletionProtection())
                    .engine(getEngine())
                    .engineVersion(getEngineVersion())
                    .globalClusterIdentifier(getGlobalClusterIdentifier())
                    .sourceDBClusterIdentifier(getSourceDbClusterIdentifier())
                    .storageEncrypted(getStorageEncrypted())
        );

        setArn(response.globalCluster().globalClusterArn());
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        DbGlobalClusterResource current = (DbGlobalClusterResource) config;
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
