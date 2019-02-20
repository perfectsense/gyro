package beam.aws.rds;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBParameterGroupStatus;
import software.amazon.awssdk.services.rds.model.DBSecurityGroupMembership;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DomainMembership;
import software.amazon.awssdk.services.rds.model.OptionGroupMembership;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceName("db-instance")
public class DbInstanceResource extends RdsTaggableResource {

    private Integer allocatedStorage;
    private Boolean allowMajorVersionUpgrade;
    private Boolean applyImmediately;
    private Boolean autoMinorVersionUpgrade;
    private String availabilityZone;
    private Integer backupRetentionPeriod;
    private String characterSetName;
    private Boolean copyTagsToSnapshot;
    private String dbClusterIdentifier;
    private String dbInstanceClass;
    private String dbInstanceIdentifier;
    private String dbName;
    private String dbParameterGroupName;
    private List<String> dbSecurityGroups;
    private String dbSubnetGroupName;
    private Boolean deleteAutomatedBackups;
    private Boolean deletionProtection;
    private String domain;
    private String domainIamRoleName;
    private List<String> enableCloudwatchLogsExports;
    private Boolean enableIamDatabaseAuthentication;
    private Boolean enablePerformanceInsights;
    private String engine;
    private String engineVersion;
    private String finalDbSnapshotIdentifier;
    private Integer iops;
    private String kmsKeyId;
    private String licenseModel;
    private String masterUserPassword;
    private String masterUsername;
    private Integer monitoringInterval;
    private String monitoringRoleArn;
    private Boolean multiAZ;
    private String optionGroupName;
    private String performanceInsightsKMSKeyId;
    private Integer performanceInsightsRetentionPeriod;
    private Integer port;
    private String preferredBackupWindow;
    private String preferredMaintenanceWindow;
    private Integer promotionTier;
    private Boolean publiclyAccessible;
    private Boolean skipFinalSnapshot;
    private Boolean storageEncrypted;
    private String storageType;
    private String tdeCredentialArn;
    private String tdeCredentialPassword;
    private String timezone;
    private List<String> vpcSecurityGroupIds;

    /**
     * The amount of storage (in gibibytes) to allocate for the DB instance.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getAllocatedStorage() {
        return allocatedStorage;
    }

    public void setAllocatedStorage(Integer allocatedStorage) {
        this.allocatedStorage = allocatedStorage;
    }

    /**
     * Indicates that minor engine upgrades are applied automatically to the DB instance during the maintenance window.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getAutoMinorVersionUpgrade() {
        return autoMinorVersionUpgrade;
    }

    public void setAutoMinorVersionUpgrade(Boolean autoMinorVersionUpgrade) {
        this.autoMinorVersionUpgrade = autoMinorVersionUpgrade;
    }

    /**
     * The EC2 Availability Zone that the DB instance is created in.
     */
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     * The number of days for which automated backups are retained.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getBackupRetentionPeriod() {
        return backupRetentionPeriod;
    }

    public void setBackupRetentionPeriod(Integer backupRetentionPeriod) {
        this.backupRetentionPeriod = backupRetentionPeriod;
    }

    /**
     * Indicates that the DB instance should be associated with the specified CharacterSet.
     */
    public String getCharacterSetName() {
        return characterSetName;
    }

    public void setCharacterSetName(String characterSetName) {
        this.characterSetName = characterSetName;
    }

    /**
     * Copy all tags from the DB instance to snapshots of the DB instance. The default is false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getCopyTagsToSnapshot() {
        return copyTagsToSnapshot;
    }

    public void setCopyTagsToSnapshot(Boolean copyTagsToSnapshot) {
        this.copyTagsToSnapshot = copyTagsToSnapshot;
    }

    /**
     * The identifier of the DB cluster that the instance will belong to.
     */
    public String getDbClusterIdentifier() {
        return dbClusterIdentifier;
    }

    public void setDbClusterIdentifier(String dbClusterIdentifier) {
        this.dbClusterIdentifier = dbClusterIdentifier;
    }

    /**
     * The compute and memory capacity of the DB instance.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDbInstanceClass() {
        return dbInstanceClass;
    }

    public void setDbInstanceClass(String dbInstanceClass) {
        this.dbInstanceClass = dbInstanceClass;
    }

    /**
     * The DB instance identifier. This parameter is stored as a lowercase string.
     */
    public String getDbInstanceIdentifier() {
        return dbInstanceIdentifier;
    }

    public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
        this.dbInstanceIdentifier = dbInstanceIdentifier;
    }

    /**
     * The name of the database to create when the DB instance is created (differs according to the database engine used). See `CreateDBInstance <https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html>`_.
     */
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * The name of the DB parameter group to associate with this DB instance. If this argument is omitted, the default DBParameterGroup for the specified engine is used.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDbParameterGroupName() {
        return dbParameterGroupName;
    }

    public void setDbParameterGroupName(String dbParameterGroupName) {
        this.dbParameterGroupName = dbParameterGroupName;
    }

    /**
     * A list of DB security groups to associate with this DB instance.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getDbSecurityGroups() {
        return dbSecurityGroups;
    }

    public void setDbSecurityGroups(List<String> dbSecurityGroups) {
        this.dbSecurityGroups = dbSecurityGroups;
    }

    /**
     * A DB subnet group to associate with this DB instance. If there is no DB subnet group, then it is a non-VPC DB instance.
     */
    public String getDbSubnetGroupName() {
        return dbSubnetGroupName;
    }

    public void setDbSubnetGroupName(String dbSubnetGroupName) {
        this.dbSubnetGroupName = dbSubnetGroupName;
    }

    /**
     * Indicates if the DB instance should have deletion protection enabled. The database can't be deleted when this value is set to true. The default is false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDeletionProtection() {
        return deletionProtection;
    }

    public void setDeletionProtection(Boolean deletionProtection) {
        this.deletionProtection = deletionProtection;
    }

    /**
     * Specify the Active Directory Domain to create the instance in.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Specify the name of the IAM role to be used when making API calls to the Directory Service.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDomainIamRoleName() {
        return domainIamRoleName;
    }

    public void setDomainIamRoleName(String domainIamRoleName) {
        this.domainIamRoleName = domainIamRoleName;
    }

    /**
     * The list of log types that need to be enabled for exporting to CloudWatch Logs. The values in the list depend on the DB engine being used. See `Publishing Database Logs to Amazon CloudWatch Logs <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_LogAccess.html#USER_LogAccess.Procedural.UploadtoCloudWatch>`_.
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
     * True to enable Performance Insights for the DB instance, and otherwise false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnablePerformanceInsights() {
        return enablePerformanceInsights;
    }

    public void setEnablePerformanceInsights(Boolean enablePerformanceInsights) {
        this.enablePerformanceInsights = enablePerformanceInsights;
    }

    /**
     * The name of the database engine to be used for this instance. See `CreateDBInstance <https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html>`_.
     */
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * The version number of the database engine to use.
     */
    @ResourceDiffProperty(updatable = true)
    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    /**
     * The amount of Provisioned IOPS (input/output operations per second) to be initially allocated for the DB instance.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getIops() {
        return iops;
    }

    public void setIops(Integer iops) {
        this.iops = iops;
    }

    /**
     * The AWS KMS key identifier for an encrypted DB instance.
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    /**
     * License model information for this DB instance. Valid values: `license-included`, `bring-your-own-license`, `general-public-license`.
     */
    @ResourceDiffProperty(updatable = true)
    public String getLicenseModel() {
        return licenseModel;
    }

    public void setLicenseModel(String licenseModel) {
        this.licenseModel = licenseModel;
    }

    /**
     * The password for the master user.
     */
    @ResourceDiffProperty(updatable = true)
    public String getMasterUserPassword() {
        return masterUserPassword;
    }

    public void setMasterUserPassword(String masterUserPassword) {
        this.masterUserPassword = masterUserPassword;
    }

    /**
     * The name for the master user.
     */
    public String getMasterUsername() {
        return masterUsername;
    }

    public void setMasterUsername(String masterUsername) {
        this.masterUsername = masterUsername;
    }

    /**
     * The interval, in seconds, between points when Enhanced Monitoring metrics are collected for the DB instance. The default is 0. Valid Values: `0`, `1`, `5`, `10`, `15`, `30`, `60`
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getMonitoringInterval() {
        return monitoringInterval;
    }

    public void setMonitoringInterval(Integer monitoringInterval) {
        this.monitoringInterval = monitoringInterval;
    }

    /**
     * The ARN for the IAM role that permits RDS to send enhanced monitoring metrics to Amazon CloudWatch Logs.
     */
    @ResourceDiffProperty(updatable = true)
    public String getMonitoringRoleArn() {
        return monitoringRoleArn;
    }

    public void setMonitoringRoleArn(String monitoringRoleArn) {
        this.monitoringRoleArn = monitoringRoleArn;
    }

    /**
     * A value that specifies whether the DB instance is a Multi-AZ deployment.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getMultiAZ() {
        return multiAZ;
    }

    public void setMultiAZ(Boolean multiAZ) {
        this.multiAZ = multiAZ;
    }

    /**
     * Indicates that the DB instance should be associated with the specified option group.
     */
    @ResourceDiffProperty(updatable = true)
    public String getOptionGroupName() {
        return optionGroupName;
    }

    public void setOptionGroupName(String optionGroupName) {
        this.optionGroupName = optionGroupName;
    }

    /**
     * The AWS KMS key identifier for encryption of Performance Insights data.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPerformanceInsightsKMSKeyId() {
        return performanceInsightsKMSKeyId;
    }

    public void setPerformanceInsightsKMSKeyId(String performanceInsightsKMSKeyId) {
        this.performanceInsightsKMSKeyId = performanceInsightsKMSKeyId;
    }

    /**
     * The amount of time, in days, to retain Performance Insights data. Valid values are `7` or `731` (2 years).
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPerformanceInsightsRetentionPeriod() {
        return performanceInsightsRetentionPeriod;
    }

    public void setPerformanceInsightsRetentionPeriod(Integer performanceInsightsRetentionPeriod) {
        this.performanceInsightsRetentionPeriod = performanceInsightsRetentionPeriod;
    }

    /**
     * The port number on which the database accepts connections.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * The daily time range during which automated backups are created if automated backups are enabled.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPreferredBackupWindow() {
        return preferredBackupWindow;
    }

    public void setPreferredBackupWindow(String preferredBackupWindow) {
        this.preferredBackupWindow = preferredBackupWindow;
    }

    /**
     * The time range each week during which system maintenance can occur, in Universal Coordinated Time (UTC). Format: ddd:hh24:mi-ddd:hh24:mi.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPreferredMaintenanceWindow() {
        return preferredMaintenanceWindow;
    }

    public void setPreferredMaintenanceWindow(String preferredMaintenanceWindow) {
        this.preferredMaintenanceWindow = preferredMaintenanceWindow;
    }

    /**
     * A value that specifies the order in which an Aurora Replica is promoted to the primary instance after a failure of the existing primary instance. Valid Values: 0 - 15.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPromotionTier() {
        return promotionTier;
    }

    public void setPromotionTier(Integer promotionTier) {
        this.promotionTier = promotionTier;
    }

    /**
     * Specifies the accessibility options for the DB instance.
     */
    public Boolean getPubliclyAccessible() {
        return publiclyAccessible;
    }

    public void setPubliclyAccessible(Boolean publiclyAccessible) {
        this.publiclyAccessible = publiclyAccessible;
    }

    /**
     * Specifies whether the DB instance is encrypted.
     */
    public Boolean getStorageEncrypted() {
        return storageEncrypted;
    }

    public void setStorageEncrypted(Boolean storageEncrypted) {
        this.storageEncrypted = storageEncrypted;
    }

    /**
     * Specifies the storage type to be associated with the DB instance. Valid values: `standard`, `gp2`, `io1`.
     */
    @ResourceDiffProperty(updatable = true)
    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    /**
     * The ARN from the key store with which to associate the instance for TDE encryption.
     */
    public String getTdeCredentialArn() {
        return tdeCredentialArn;
    }

    public void setTdeCredentialArn(String tdeCredentialArn) {
        this.tdeCredentialArn = tdeCredentialArn;
    }

    /**
     * The password for the given ARN from the key store in order to access the device.
     */
    public String getTdeCredentialPassword() {
        return tdeCredentialPassword;
    }

    public void setTdeCredentialPassword(String tdeCredentialPassword) {
        this.tdeCredentialPassword = tdeCredentialPassword;
    }

    /**
     * The time zone of the DB instance. The time zone parameter is currently supported only by Microsoft SQL Server.
     */
    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * A list of Amazon EC2 VPC security groups to associate with this DB in
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getVpcSecurityGroupIds() {
        return vpcSecurityGroupIds;
    }

    public void setVpcSecurityGroupIds(List<String> vpcSecurityGroupIds) {
        this.vpcSecurityGroupIds = vpcSecurityGroupIds;
    }

    /**
     * Indicates that major version upgrades are allowed.
     */
    public Boolean getAllowMajorVersionUpgrade() {
        return allowMajorVersionUpgrade;
    }

    public void setAllowMajorVersionUpgrade(Boolean allowMajorVersionUpgrade) {
        this.allowMajorVersionUpgrade = allowMajorVersionUpgrade;
    }

    /**
     * Specifies whether the modifications in this request and any pending modifications are asynchronously applied as soon as possible, regardless of the `PreferredMaintenanceWindow` setting for the DB instance.
     */
    public Boolean getApplyImmediately() {
        return applyImmediately;
    }

    public void setApplyImmediately(Boolean applyImmediately) {
        this.applyImmediately = applyImmediately;
    }

    /**
     * The `DBSnapshotIdentifier` of the new DB snapshot created when `SkipFinalSnapshot` is set to false.
     */
    public String getFinalDbSnapshotIdentifier() {
        return finalDbSnapshotIdentifier;
    }

    public void setFinalDbSnapshotIdentifier(String finalDbSnapshotIdentifier) {
        this.finalDbSnapshotIdentifier = finalDbSnapshotIdentifier;
    }

    /**
     * A value that indicates whether a final DB snapshot is created before the DB instance is deleted.
     */
    public Boolean getSkipFinalSnapshot() {
        return skipFinalSnapshot;
    }

    public void setSkipFinalSnapshot(Boolean skipFinalSnapshot) {
        this.skipFinalSnapshot = skipFinalSnapshot;
    }

    /**
     * A value that indicates whether to remove automated backups immediately after the DB instance is deleted.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getDeleteAutomatedBackups() {
        return deleteAutomatedBackups;
    }

    public void setDeleteAutomatedBackups(Boolean deleteAutomatedBackups) {
        this.deleteAutomatedBackups = deleteAutomatedBackups;
    }

    @Override
    public boolean doRefresh() {
        RdsClient client = createClient(RdsClient.class);

        if (ObjectUtils.isBlank(getDbInstanceIdentifier())) {
            throw new BeamException("db-instance-identifier is missing, unable to load db instance.");
        }

        try {
            DescribeDbInstancesResponse response = client.describeDBInstances(
                r -> r.dbInstanceIdentifier(getDbInstanceIdentifier())
            );

            response.dbInstances().stream()
                .forEach(i -> {
                        setAllocatedStorage(i.allocatedStorage());
                        setAutoMinorVersionUpgrade(i.autoMinorVersionUpgrade());
                        setAvailabilityZone(i.availabilityZone());
                        setBackupRetentionPeriod(i.backupRetentionPeriod());
                        setCharacterSetName(i.characterSetName());
                        setCopyTagsToSnapshot(i.copyTagsToSnapshot());
                        setDbClusterIdentifier(i.dbClusterIdentifier());
                        setDbInstanceClass(i.dbInstanceClass());
                        setDbInstanceIdentifier(i.dbInstanceIdentifier());
                        setDbName(i.dbName());

                        setDbParameterGroupName(i.dbParameterGroups().stream()
                            .findFirst().map(DBParameterGroupStatus::dbParameterGroupName)
                            .orElse(null));

                        setDbSecurityGroups(i.dbSecurityGroups().stream()
                            .map(DBSecurityGroupMembership::dbSecurityGroupName)
                            .collect(Collectors.toList()));

                        setDbSubnetGroupName(i.dbSubnetGroup() != null ? i.dbSubnetGroup().dbSubnetGroupName() : null);
                        setDeletionProtection(i.deletionProtection());

                        setDomain(i.domainMemberships().stream()
                            .findFirst().map(DomainMembership::domain)
                            .orElse(null));

                        setDomainIamRoleName(i.domainMemberships().stream()
                            .findFirst().map(DomainMembership::iamRoleName)
                            .orElse(null));

                        List<String> cwLogsExports = i.enabledCloudwatchLogsExports();
                        setEnableCloudwatchLogsExports(cwLogsExports.isEmpty() ? null : cwLogsExports);
                        setEnableIamDatabaseAuthentication(i.iamDatabaseAuthenticationEnabled());
                        setEnablePerformanceInsights(i.performanceInsightsEnabled());
                        setEngine(i.engine());

                        String version = i.engineVersion();
                        if (getEngineVersion() != null) {
                            version = version.substring(0, getEngineVersion().length());
                        }

                        setEngineVersion(version);
                        setIops(i.iops());
                        setKmsKeyId(i.kmsKeyId());
                        setLicenseModel(i.licenseModel());
                        setMasterUsername(i.masterUsername());
                        setMonitoringInterval(i.monitoringInterval());
                        setMonitoringRoleArn(i.monitoringRoleArn());
                        setMultiAZ(i.multiAZ());

                        setOptionGroupName(i.optionGroupMemberships().stream()
                            .findFirst().map(OptionGroupMembership::optionGroupName)
                            .orElse(null));

                        setPerformanceInsightsKMSKeyId(i.performanceInsightsKMSKeyId());
                        setPerformanceInsightsRetentionPeriod(i.performanceInsightsRetentionPeriod());
                        setPort(i.dbInstancePort());
                        setPreferredBackupWindow(i.preferredBackupWindow());
                        setPreferredMaintenanceWindow(i.preferredMaintenanceWindow());
                        setPromotionTier(i.promotionTier());
                        setPubliclyAccessible(i.publiclyAccessible());
                        setStorageEncrypted(i.storageEncrypted());
                        setStorageType(i.storageType());
                        setTdeCredentialArn(i.tdeCredentialArn());
                        setTimezone(i.timezone());
                        setVpcSecurityGroupIds(i.vpcSecurityGroups().stream()
                            .map(VpcSecurityGroupMembership::vpcSecurityGroupId)
                            .collect(Collectors.toList()));
                    }
                );

        } catch (DbInstanceNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void doCreate() {
        RdsClient client = createClient(RdsClient.class);
        client.createDBInstance(
            r -> r.allocatedStorage(getAllocatedStorage())
                    .autoMinorVersionUpgrade(getAutoMinorVersionUpgrade())
                    .availabilityZone(getAvailabilityZone())
                    .backupRetentionPeriod(getBackupRetentionPeriod())
                    .characterSetName(getCharacterSetName())
                    .copyTagsToSnapshot(getCopyTagsToSnapshot())
                    .dbClusterIdentifier(getDbClusterIdentifier())
                    .dbInstanceClass(getDbInstanceClass())
                    .dbInstanceIdentifier(getDbInstanceIdentifier())
                    .dbName(getDbName())
                    .dbParameterGroupName(getDbParameterGroupName())
                    .dbSecurityGroups(getDbSecurityGroups())
                    .dbSubnetGroupName(getDbSubnetGroupName())
                    .deletionProtection(getDeletionProtection())
                    .domain(getDomain())
                    .domainIAMRoleName(getDomainIamRoleName())
                    .enableCloudwatchLogsExports(getEnableCloudwatchLogsExports())
                    .enableIAMDatabaseAuthentication(getEnableIamDatabaseAuthentication())
                    .enablePerformanceInsights(getEnablePerformanceInsights())
                    .engine(getEngine())
                    .engineVersion(getEngineVersion())
                    .iops(getIops())
                    .kmsKeyId(getKmsKeyId())
                    .licenseModel(getLicenseModel())
                    .masterUsername(getMasterUsername())
                    .masterUserPassword(getMasterUserPassword())
                    .monitoringInterval(getMonitoringInterval())
                    .monitoringRoleArn(getMonitoringRoleArn())
                    .multiAZ(getMultiAZ())
                    .optionGroupName(getOptionGroupName())
                    .performanceInsightsKMSKeyId(getPerformanceInsightsKMSKeyId())
                    .performanceInsightsRetentionPeriod(getPerformanceInsightsRetentionPeriod())
                    .port(getPort())
                    .preferredBackupWindow(getPreferredBackupWindow())
                    .preferredMaintenanceWindow(getPreferredMaintenanceWindow())
                    .promotionTier(getPromotionTier())
                    .publiclyAccessible(getPubliclyAccessible())
                    .storageEncrypted(getStorageEncrypted())
                    .storageType(getStorageType())
                    .tdeCredentialArn(getTdeCredentialArn())
                    .tdeCredentialPassword(getTdeCredentialPassword())
                    .timezone(getTimezone())
                    .vpcSecurityGroupIds(getVpcSecurityGroupIds())
        );
    }

    @Override
    public void doUpdate(Resource current, Set<String> changedProperties) {
        RdsClient client = createClient(RdsClient.class);
        client.modifyDBInstance(
            r -> r.allocatedStorage(getAllocatedStorage())
                    .applyImmediately(getApplyImmediately())
                    .allowMajorVersionUpgrade(getAllowMajorVersionUpgrade())
                    .autoMinorVersionUpgrade(getAutoMinorVersionUpgrade())
                    .backupRetentionPeriod(getBackupRetentionPeriod())
                    .cloudwatchLogsExportConfiguration(c -> c.enableLogTypes(getEnableCloudwatchLogsExports()))
                    .copyTagsToSnapshot(getCopyTagsToSnapshot())
                    .dbInstanceClass(getDbInstanceClass())
                    .dbInstanceIdentifier(getDbInstanceIdentifier())
                    .dbParameterGroupName(getDbParameterGroupName())
                    .dbSecurityGroups(getDbSecurityGroups())
                    .dbSubnetGroupName(getDbSubnetGroupName())
                    .deletionProtection(getDeletionProtection())
                    .domain(getDomain())
                    .domainIAMRoleName(getDomainIamRoleName())
                    .enableIAMDatabaseAuthentication(getEnableIamDatabaseAuthentication())
                    .enablePerformanceInsights(getEnablePerformanceInsights())
                    .engineVersion(getEngineVersion())
                    .iops(getIops())
                    .licenseModel(getLicenseModel())
                    .masterUserPassword(getMasterUserPassword())
                    .monitoringInterval(getMonitoringInterval())
                    .monitoringRoleArn(getMonitoringRoleArn())
                    .multiAZ(getMultiAZ())
                    .optionGroupName(getOptionGroupName())
                    .performanceInsightsKMSKeyId(getPerformanceInsightsKMSKeyId())
                    .performanceInsightsRetentionPeriod(getPerformanceInsightsRetentionPeriod())
                    .preferredBackupWindow(getPreferredBackupWindow())
                    .preferredMaintenanceWindow(getPreferredMaintenanceWindow())
                    .promotionTier(getPromotionTier())
                    .publiclyAccessible(getPubliclyAccessible())
                    .storageType(getStorageType())
                    .tdeCredentialArn(getTdeCredentialArn())
                    .tdeCredentialPassword(getTdeCredentialPassword())
                    .vpcSecurityGroupIds(getVpcSecurityGroupIds())
        );
    }

    @Override
    public void delete() {
        RdsClient client = createClient(RdsClient.class);
        client.deleteDBInstance(
            r -> r.dbInstanceIdentifier(getDbInstanceIdentifier())
                    .finalDBSnapshotIdentifier(getFinalDbSnapshotIdentifier())
                    .skipFinalSnapshot(getSkipFinalSnapshot())
                    .deleteAutomatedBackups(getDeleteAutomatedBackups())
        );
    }

    @Override
    public String toDisplayString() {
        return "db instance " + getDbInstanceIdentifier();
    }
}
