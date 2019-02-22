package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbClusterResponse;
import software.amazon.awssdk.services.rds.model.DBClusterOptionGroupStatus;
import software.amazon.awssdk.services.rds.model.DbClusterNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbClusterStateException;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create a Aurora cluster.
 *
 * .. code-block:: beam
 *
 *    aws::db-cluster db-cluster-example
 *        db-cluster-identifier: "aurora-mysql-cluster"
 *        engine: "aurora-mysql"
 *        availability-zones: ["us-east-2a", "us-east-2b", "us-east-2c"]
 *        db-name: "clusterexample"
 *        master-username: "user"
 *        master-user-password: "password"
 *        backup-retention-period: 5
 *        preferred-backup-window: "07:00-09:00"
 *        delete-automated-backups: true
 *        skip-final-snapshot: true
 *        tags: {
 *            Name: "aurora-mysql-cluster1"
 *        }
 *    end
 *
 * .. code-block:: beam
 *
 *    aws::db-cluster db-cluster-serverless-example
 *        db-cluster-identifier: "aurora-serverless-cluster"
 *        engine: "aurora"
 *        engine-mode: "serverless"
 *
 *        scaling-configuration
 *            auto-pause: true
 *            max-capacity: 128
 *            min-capacity: 2
 *            seconds-until-auto-pause: 300
 *        end
 *
 *        availability-zones: ["us-east-2a", "us-east-2b", "us-east-2c"]
 *        db-name: "clusterexample"
 *        master-username: "user"
 *        master-user-password: "password"
 *        backup-retention-period: 5
 *        preferred-backup-window: "07:00-09:00"
 *        delete-automated-backups: true
 *        skip-final-snapshot: true
 *        tags: {
 *            Name: "aurora-serverless-cluster"
 *        }
 *    end
 */
@ResourceName("db-cluster")
public class DbClusterResource extends RdsTaggableResource {

    private Boolean applyImmediately;
    private List<String> availabilityZones;
    private Long backTrackWindow;
    private Integer backupRetentionPeriod;
    private String characterSetName;
    private String dbClusterIdentifier;
    private String dbName;
    private String dbClusterParameterGroupName;
    private String dbSubnetGroupName;
    private Boolean deletionProtection;
    private List<String> enableCloudwatchLogsExports;
    private Boolean enableIamDatabaseAuthentication;
    private String engine;
    private String engineMode;
    private String engineVersion;
    private String finalDbSnapshotIdentifier;
    private String globalClusterIdentifier;
    private String kmsKeyId;
    private String masterUserPassword;
    private String masterUsername;
    private String optionGroupName;
    private Integer port;
    private String preferredBackupWindow;
    private String preferredMaintenanceWindow;
    private String preSignedUrl;
    private String replicationSourceIdentifier;
    private ScalingConfiguration scalingConfiguration;
    private Boolean skipFinalSnapshot;
    private Boolean storageEncrypted;
    private List<String> vpcSecurityGroupIds;

    /**
     * Specifies whether the modifications in this request and any pending modifications are asynchronously applied as soon as possible, regardless of the `PreferredMaintenanceWindow` setting for the DB instance.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getApplyImmediately() {
        return applyImmediately;
    }

    public void setApplyImmediately(Boolean applyImmediately) {
        this.applyImmediately = applyImmediately;
    }

    /**
     * A list of EC2 Availability Zones that instances in the DB cluster can be created in.
     */
    public List<String> getAvailabilityZones() {
        if (availabilityZones == null) {
            availabilityZones = new ArrayList<>();
        }

        return availabilityZones;
    }

    public void setAvailabilityZones(List<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    /**
     * The target backtrack window, in seconds. To disable backtracking, set this value to ``0``.
     */
    @ResourceDiffProperty(updatable = true)
    public Long getBackTrackWindow() {
        return backTrackWindow;
    }

    public void setBackTrackWindow(Long backTrackWindow) {
        this.backTrackWindow = backTrackWindow;
    }

    /**
     * The number of days for which automated backups are retained. You must specify a minimum value of ``1``.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getBackupRetentionPeriod() {
        return backupRetentionPeriod;
    }

    public void setBackupRetentionPeriod(Integer backupRetentionPeriod) {
        this.backupRetentionPeriod = backupRetentionPeriod;
    }

    /**
     * A value that indicates that the DB cluster should be associated with the specified CharacterSet.
     */
    public String getCharacterSetName() {
        return characterSetName;
    }

    public void setCharacterSetName(String characterSetName) {
        this.characterSetName = characterSetName;
    }

    /**
     * The DB cluster identifier. This parameter is stored as a lowercase string.
     */
    public String getDbClusterIdentifier() {
        return dbClusterIdentifier;
    }

    public void setDbClusterIdentifier(String dbClusterIdentifier) {
        this.dbClusterIdentifier = dbClusterIdentifier;
    }

    /**
     * The name for your database of up to 64 alpha-numeric characters. If you do not provide a name, Amazon RDS will not create a database in the DB cluster you are creating.
     */
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * The name of the DB cluster parameter group to associate with this DB cluster. If this argument is omitted, ``default.aurora5.6`` is used.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDbClusterParameterGroupName() {
        return dbClusterParameterGroupName;
    }

    public void setDbClusterParameterGroupName(String dbClusterParameterGroupName) {
        this.dbClusterParameterGroupName = dbClusterParameterGroupName;
    }

    /**
     * A DB subnet group to associate with this DB cluster.
     */
    public String getDbSubnetGroupName() {
        return dbSubnetGroupName;
    }

    public void setDbSubnetGroupName(String dbSubnetGroupName) {
        this.dbSubnetGroupName = dbSubnetGroupName;
    }

    /**
     * Indicates if the DB cluster should have deletion protection enabled. The database can't be deleted when this value is set to true. The default is false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDeletionProtection() {
        return deletionProtection;
    }

    public void setDeletionProtection(Boolean deletionProtection) {
        this.deletionProtection = deletionProtection;
    }

    /**
     * The list of log types that need to be enabled for exporting to CloudWatch Logs.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getEnableCloudwatchLogsExports() {
        return enableCloudwatchLogsExports;
    }

    public void setEnableCloudwatchLogsExports(List<String> enableCloudwatchLogsExports) {
        this.enableCloudwatchLogsExports = enableCloudwatchLogsExports;
    }

    /**
     * True to enable mapping of AWS Identity and Access Management (IAM) accounts to database accounts, and otherwise false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableIamDatabaseAuthentication() {
        return enableIamDatabaseAuthentication;
    }

    public void setEnableIamDatabaseAuthentication(Boolean enableIamDatabaseAuthentication) {
        this.enableIamDatabaseAuthentication = enableIamDatabaseAuthentication;
    }

    /**
     * The name of the database engine to be used for this DB cluster. Valid Values: ``aurora`` (for MySQL 5.6-compatible Aurora), ``aurora-mysql`` (for MySQL 5.7-compatible Aurora), and ``aurora-postgresql``. (Required)
     */
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * The DB engine mode of the DB cluster, either ``provisioned``, ``serverless``, ``parallelquery``, or ``global``.
     */
    public String getEngineMode() {
        return engineMode;
    }

    public void setEngineMode(String engineMode) {
        this.engineMode = engineMode;
    }

    /**
     * The version number of the database engine to use.
     */
    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    /**
     * The DB cluster snapshot identifier of the new DB cluster snapshot created when `SkipFinalSnapshot` is set to false.
     */
    public String getFinalDbSnapshotIdentifier() {
        return finalDbSnapshotIdentifier;
    }

    public void setFinalDbSnapshotIdentifier(String finalDbSnapshotIdentifier) {
        this.finalDbSnapshotIdentifier = finalDbSnapshotIdentifier;
    }

    /**
     * The global cluster ID of an Aurora cluster that becomes the primary cluster in the new global database cluster.
     */
    public String getGlobalClusterIdentifier() {
        return globalClusterIdentifier;
    }

    public void setGlobalClusterIdentifier(String globalClusterIdentifier) {
        this.globalClusterIdentifier = globalClusterIdentifier;
    }

    /**
     * The AWS KMS key identifier for an encrypted DB cluster.
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    /**
     * The password for the master database user.
     */
    @ResourceDiffProperty(updatable = true)
    public String getMasterUserPassword() {
        return masterUserPassword;
    }

    public void setMasterUserPassword(String masterUserPassword) {
        this.masterUserPassword = masterUserPassword;
    }

    /**
     * The name of the master user for the DB cluster.
     */
    public String getMasterUsername() {
        return masterUsername;
    }

    public void setMasterUsername(String masterUsername) {
        this.masterUsername = masterUsername;
    }

    /**
     * A value that indicates that the DB cluster should be associated with the specified option group.
     */
    @ResourceDiffProperty(updatable = true)
    public String getOptionGroupName() {
        return optionGroupName;
    }

    public void setOptionGroupName(String optionGroupName) {
        this.optionGroupName = optionGroupName;
    }

    /**
     * The port number on which the instances in the DB cluster accept connections. Default: ``3306`` if engine is set as aurora or ``5432`` if set to aurora-postgresql.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * The daily time range during which automated backups are created if automated backups are enabled using the `BackupRetentionPeriod` parameter.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPreferredBackupWindow() {
        return preferredBackupWindow;
    }

    public void setPreferredBackupWindow(String preferredBackupWindow) {
        this.preferredBackupWindow = preferredBackupWindow;
    }

    /**
     * The weekly time range during which system maintenance can occur, in Universal Coordinated Time (UTC). Format: ddd:hh24:mi-ddd:hh24:mi
     */
    @ResourceDiffProperty(updatable = true)
    public String getPreferredMaintenanceWindow() {
        return preferredMaintenanceWindow;
    }

    public void setPreferredMaintenanceWindow(String preferredMaintenanceWindow) {
        this.preferredMaintenanceWindow = preferredMaintenanceWindow;
    }

    /**
     * A URL that contains a Signature Version 4 signed request for the `CreateDBCluster` action to be called in the source AWS Region where the DB cluster is replicated from.
     */
    public String getPreSignedUrl() {
        return preSignedUrl;
    }

    public void setPreSignedUrl(String preSignedUrl) {
        this.preSignedUrl = preSignedUrl;
    }

    /**
     * The Amazon Resource Name (ARN) of the source DB instance or DB cluster if this DB cluster is created as a Read Replica.
     */
    @ResourceDiffProperty(updatable = true)
    public String getReplicationSourceIdentifier() {
        return replicationSourceIdentifier;
    }

    public void setReplicationSourceIdentifier(String replicationSourceIdentifier) {
        this.replicationSourceIdentifier = replicationSourceIdentifier;
    }

    /**
     * For DB clusters in `serverless` DB engine mode, the scaling properties of the DB cluster.
     */
    @ResourceDiffProperty(updatable = true)
    public ScalingConfiguration getScalingConfiguration() {
        return scalingConfiguration;
    }

    public void setScalingConfiguration(ScalingConfiguration scalingConfiguration) {
        this.scalingConfiguration = scalingConfiguration;
    }

    /**
     * Determines whether a final DB cluster snapshot is created before the DB cluster is deleted.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getSkipFinalSnapshot() {
        return skipFinalSnapshot;
    }

    public void setSkipFinalSnapshot(Boolean skipFinalSnapshot) {
        this.skipFinalSnapshot = skipFinalSnapshot;
    }

    /**
     * Specifies whether the DB cluster is encrypted.
     */
    public Boolean getStorageEncrypted() {
        return storageEncrypted;
    }

    public void setStorageEncrypted(Boolean storageEncrypted) {
        this.storageEncrypted = storageEncrypted;
    }

    /**
     * A list of EC2 VPC security groups to associate with this DB cluster.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getVpcSecurityGroupIds() {
        return vpcSecurityGroupIds;
    }

    public void setVpcSecurityGroupIds(List<String> vpcSecurityGroupIds) {
        this.vpcSecurityGroupIds = vpcSecurityGroupIds;
    }

    @Override
    protected boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getDbClusterIdentifier())) {
            throw new BeamException("db-cluster-identifier is missing, unable to load db cluster.");
        }

        try {
            DescribeDbClustersResponse response = client.describeDBClusters(
                r -> r.dbClusterIdentifier(getDbClusterIdentifier())
            );

            response.dbClusters().stream()
                .forEach(c -> {
                        setAvailabilityZones(c.availabilityZones());
                        setBackTrackWindow(c.backtrackWindow());
                        setBackupRetentionPeriod(c.backupRetentionPeriod());
                        setCharacterSetName(c.characterSetName());
                        setDbClusterParameterGroupName(c.dbClusterParameterGroup());
                        setDbName(c.databaseName());
                        setDbSubnetGroupName(c.dbSubnetGroup());
                        setDeletionProtection(c.deletionProtection());

                        List<String> cwLogsExports = c.enabledCloudwatchLogsExports();
                        setEnableCloudwatchLogsExports(cwLogsExports.isEmpty() ? null : cwLogsExports);
                        setEnableIamDatabaseAuthentication(c.iamDatabaseAuthenticationEnabled());
                        setEngine(c.engine());

                        String version = c.engineVersion();
                        if (getEngineVersion() != null) {
                            version = version.substring(0, getEngineVersion().length());
                        }

                        setEngineVersion(version);
                        setEngineMode(c.engineMode());
                        setKmsKeyId(c.kmsKeyId());
                        setMasterUsername(c.masterUsername());

                        setOptionGroupName(c.dbClusterOptionGroupMemberships().stream()
                            .findFirst().map(DBClusterOptionGroupStatus::dbClusterOptionGroupName)
                            .orElse(null));

                        setPort(c.port());
                        setPreferredBackupWindow(c.preferredBackupWindow());
                        setPreferredMaintenanceWindow(c.preferredMaintenanceWindow());
                        setReplicationSourceIdentifier(c.replicationSourceIdentifier());

                        if (c.scalingConfigurationInfo() != null) {
                            ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
                            scalingConfiguration.setAutoPause(c.scalingConfigurationInfo().autoPause());
                            scalingConfiguration.setMaxCapacity(c.scalingConfigurationInfo().maxCapacity());
                            scalingConfiguration.setMinCapacity(c.scalingConfigurationInfo().minCapacity());
                            scalingConfiguration.setSecondsUntilAutoPause(c.scalingConfigurationInfo().secondsUntilAutoPause());
                            setScalingConfiguration(scalingConfiguration);
                        }

                        setStorageEncrypted(c.storageEncrypted());

                        setVpcSecurityGroupIds(c.vpcSecurityGroups().stream()
                            .map(VpcSecurityGroupMembership::vpcSecurityGroupId)
                            .collect(Collectors.toList()));
                    }
                );

        } catch (DbClusterNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        software.amazon.awssdk.services.rds.model.ScalingConfiguration scalingConfiguration = getScalingConfiguration() != null
            ? software.amazon.awssdk.services.rds.model.ScalingConfiguration.builder()
                .autoPause(getScalingConfiguration().getAutoPause())
                .maxCapacity(getScalingConfiguration().getMaxCapacity())
                .minCapacity(getScalingConfiguration().getMinCapacity())
                .secondsUntilAutoPause(getScalingConfiguration().getSecondsUntilAutoPause())
                .build()
            : null;

        CreateDbClusterResponse response = client.createDBCluster(
            r -> r.availabilityZones(getAvailabilityZones())
                    .backtrackWindow(getBackTrackWindow())
                    .backupRetentionPeriod(getBackupRetentionPeriod())
                    .characterSetName(getCharacterSetName())
                    .databaseName(getDbName())
                    .dbClusterIdentifier(getDbClusterIdentifier())
                    .dbClusterParameterGroupName(getDbClusterParameterGroupName())
                    .dbSubnetGroupName(getDbSubnetGroupName())
                    .deletionProtection(getDeletionProtection())
                    .enableCloudwatchLogsExports(getEnableCloudwatchLogsExports())
                    .enableIAMDatabaseAuthentication(getEnableIamDatabaseAuthentication())
                    .engine(getEngine())
                    .engineVersion(getEngineVersion())
                    .engineMode(getEngineMode())
                    .globalClusterIdentifier(getGlobalClusterIdentifier())
                    .kmsKeyId(getKmsKeyId())
                    .masterUsername(getMasterUsername())
                    .masterUserPassword(getMasterUserPassword())
                    .optionGroupName(getOptionGroupName())
                    .port(getPort())
                    .preferredBackupWindow(getPreferredBackupWindow())
                    .preferredMaintenanceWindow(getPreferredMaintenanceWindow())
                    .preSignedUrl(getPreSignedUrl())
                    .replicationSourceIdentifier(getReplicationSourceIdentifier())
                    .scalingConfiguration(scalingConfiguration)
                    .storageEncrypted(getStorageEncrypted())
                    .vpcSecurityGroupIds(getVpcSecurityGroupIds())
        );

        setArn(response.dbCluster().dbClusterArn());
    }

    @Override
    protected void doUpdate(Resource config, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        DbClusterResource current = (DbClusterResource) config;
        software.amazon.awssdk.services.rds.model.ScalingConfiguration scalingConfiguration = getScalingConfiguration() != null
            ? software.amazon.awssdk.services.rds.model.ScalingConfiguration.builder()
                .autoPause(getScalingConfiguration().getAutoPause())
                .maxCapacity(getScalingConfiguration().getMaxCapacity())
                .minCapacity(getScalingConfiguration().getMinCapacity())
                .secondsUntilAutoPause(getScalingConfiguration().getSecondsUntilAutoPause())
                .build()
            : null;

        try {
            client.modifyDBCluster(
                r -> r.applyImmediately(getApplyImmediately())
                    .backtrackWindow(getBackTrackWindow())
                    .backupRetentionPeriod(getBackupRetentionPeriod())
                    .cloudwatchLogsExportConfiguration(c -> c.enableLogTypes(getEnableCloudwatchLogsExports()))
                    .dbClusterIdentifier(current.getDbClusterIdentifier())
                    .dbClusterParameterGroupName(getDbClusterParameterGroupName())
                    .deletionProtection(getDeletionProtection())
                    .enableIAMDatabaseAuthentication(Objects.equals(getEnableIamDatabaseAuthentication(), current.getEnableIamDatabaseAuthentication())
                        ? null : getEnableIamDatabaseAuthentication())

                    .engineVersion(Objects.equals(getEngineVersion(), current.getEngineVersion()) ? null : getEngineVersion())
                    .masterUserPassword(getMasterUserPassword())
                    .newDBClusterIdentifier(getDbClusterIdentifier())
                    .optionGroupName(getOptionGroupName())
                    .port(Objects.equals(getPort(), current.getPort()) ? null : getPort())
                    .preferredBackupWindow(Objects.equals(getPreferredBackupWindow(), current.getPreferredBackupWindow())
                        ? null : getPreferredBackupWindow())
                    .preferredMaintenanceWindow(Objects.equals(getPreferredMaintenanceWindow(), current.getPreferredMaintenanceWindow())
                        ? null : getPreferredMaintenanceWindow())
                    .scalingConfiguration(scalingConfiguration)
                    .vpcSecurityGroupIds(getVpcSecurityGroupIds())
            );
        } catch (InvalidDbClusterStateException ex) {
            throw new BeamException(ex.getLocalizedMessage());
        }
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        if (getGlobalClusterIdentifier() != null) {
            client.removeFromGlobalCluster(
                r -> r.dbClusterIdentifier(getArn())
                        .globalClusterIdentifier(getGlobalClusterIdentifier())
            );
        }

        client.deleteDBCluster(
            r -> r.dbClusterIdentifier(getDbClusterIdentifier())
                    .finalDBSnapshotIdentifier(getFinalDbSnapshotIdentifier())
                    .skipFinalSnapshot(getSkipFinalSnapshot())
        );
    }

    @Override
    public String toDisplayString() {
        return "db cluster " + getDbClusterIdentifier();
    }
}
