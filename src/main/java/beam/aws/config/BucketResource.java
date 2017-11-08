package beam.aws.config;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamException;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.psddev.dari.util.ObjectUtils;
import org.fusesource.jansi.AnsiRenderWriter;

public class BucketResource extends AWSResource<Bucket> implements Taggable {

    private String name;
    private List<CORSRule> corsRules;
    private Set<S3ObjectResource> s3Objects;
    private Map<String, String> tags;
    private String replicateTo;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass, AWSCredentialsProvider provider) {

        try {
            Constructor<?> constructor = clientClass.getConstructor(AWSCredentialsProvider.class);

            T client = (T) constructor.newInstance(provider);

            if (getRegion() != null) {
                client.setRegion(getRegion());
            }

            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Never {@code null}.
     */
    public List<CORSRule> getCorsRules() {
        if (corsRules == null) {
            corsRules = new ArrayList<>();
        }
        return corsRules;
    }

    public void setCorsRules(List<CORSRule> corsRules) {
        this.corsRules = corsRules;
    }

    @ResourceDiffProperty(updatable = true)
    public String getCorsRulesString() {
        if (getCorsRules().size() > 0) {
            return ObjectUtils.toJson(getCorsRules());
        }

        return null;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<S3ObjectResource> getS3Objects() {
        if (s3Objects == null) {
            s3Objects = new HashSet<>();
        }
        return s3Objects;
    }

    public void setS3Objects(Set<S3ObjectResource> s3Objects) {
        this.s3Objects = s3Objects;
    }

    @ResourceDiffProperty(updatable = true)
    public String getReplicateTo() {
        if (replicateTo == null) {
            replicateTo = "None";
        }

        return replicateTo;
    }

    public void setReplicateTo(String replicateTo) {
        this.replicateTo = replicateTo;
    }

    @Override
    public String awsId() {
        return getName();
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, Bucket bucket) {
        AmazonS3Client s3Client = createClient(AmazonS3Client.class, cloud.getProvider());
        String name = bucket.getName();

        setName(name);

        try {
            BucketCrossOriginConfiguration cors = s3Client.getBucketCrossOriginConfiguration(name);

            if (cors != null) {
                setCorsRules(cors.getRules());
            }
        } catch (AmazonS3Exception as3) {

        }

        try {
            BucketTaggingConfiguration taggingConfiguration = s3Client.getBucketTaggingConfiguration(name);
            if (taggingConfiguration != null && taggingConfiguration.getTagSet() != null) {
                this.tags = taggingConfiguration.getTagSet().getAllTags();
            }
        } catch (AmazonS3Exception ase) {
            if (ase.getStatusCode() == 403) {
                throw new BeamException("Access Denied to bucket '" + name + "'");
            }

            throw ase;
        }

        try {
            String destination = s3Client.getBucketReplicationConfiguration(getName()).getRule("replication-rule").getDestinationConfig().getBucketARN();

            int index = destination.lastIndexOf(':');
            destination = destination.substring(index+1);

            setReplicateTo(destination);

        } catch (Exception error) {
            setReplicateTo("None");
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getS3Objects());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Bucket> current) throws Exception {
        BucketResource currentBucket = (BucketResource) current;

        update.update(currentBucket.getS3Objects(), getS3Objects());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getS3Objects());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonS3Client s3Client = createClient(AmazonS3Client.class, cloud.getProvider());
        String name = getName();
        List<CORSRule> corsRules = getCorsRules();

        try {
            s3Client.createBucket(getName());
        } catch (AmazonS3Exception ase) {
            if (ase.getStatusCode() == 409) {
                throw new BeamException("An operation on bucket '" + name + "' is pending");
            }

            throw ase;
        }

        if (!corsRules.isEmpty()) {
            s3Client.setBucketCrossOriginConfiguration(name, new BucketCrossOriginConfiguration(corsRules));
        }

        if (!tags.isEmpty()) {
            TagSet tagSet = new TagSet(tags);
            BucketTaggingConfiguration taggingConfiguration = new BucketTaggingConfiguration(Collections.singleton(tagSet));

            s3Client.setBucketTaggingConfiguration(name, taggingConfiguration);
        }

        if (!"None".equals(getReplicateTo())) {
            startReplication(getName(), getReplicateTo(), s3Client, cloud);
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Bucket> current, Set<String> changedProperties) {
        AmazonS3Client s3Client = createClient(AmazonS3Client.class, cloud.getProvider());
        String name = getName();
        List<CORSRule> corsRules = getCorsRules();

        if (!corsRules.isEmpty()) {
            s3Client.setBucketCrossOriginConfiguration(name, new BucketCrossOriginConfiguration(corsRules));
        } else {
            s3Client.deleteBucketCrossOriginConfiguration(name);
        }

        if (!tags.isEmpty()) {
            TagSet tagSet = new TagSet(tags);
            BucketTaggingConfiguration taggingConfiguration = new BucketTaggingConfiguration(Collections.singleton(tagSet));

            s3Client.setBucketTaggingConfiguration(name, taggingConfiguration);
        } else {
            s3Client.deleteBucketTaggingConfiguration(name);
        }

        String currentReplication = ((BucketResource)current).getReplicateTo();

        if (!"None".equals(getReplicateTo()) && !"None".equals(currentReplication) && !getReplicateTo().equals(currentReplication)) {
            stopReplication(getName(), currentReplication, s3Client);
            startReplication(getName(), getReplicateTo(), s3Client, cloud);

        } else if (!"None".equals(getReplicateTo()) && "None".equals(currentReplication)) {
            startReplication(getName(), getReplicateTo(), s3Client, cloud);

        } else if ("None".equals(getReplicateTo()) && !"None".equals(currentReplication)) {
            stopReplication(getName(), currentReplication, s3Client);
        }
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AWSCloud cloud) {
    }

    @Override
    public String toDisplayString() {
        return "bucket " + getName();
    }

    public static final void displayCopyMessage(String key) {
        PrintWriter out = new AnsiRenderWriter(System.out, true);
        out.print("\n@|yellow Copying " + key + " ...|@");
        out.flush();
    }

    private void createReplicationRole(String source, String destination, String roleName, AWSCloud cloud) {
        RoleResource roleResource = new RoleResource();
        roleResource.setRoleName(roleName);

        try {
            roleResource.setAssumeRolePolicyDocument((Map<String, Object>) ObjectUtils.fromJson("{\"Version\": \"2012-10-17\",\"Statement\": " +
                    "[{\"Sid\": \"\",\"Effect\": \"Allow\",\"Principal\": {\"Service\": \"s3.amazonaws.com\"},\"Action\": \"sts:AssumeRole\"}]}"));

        RolePolicyResource policyResource = new RolePolicyResource();

        String policyDocumentString = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Action\": " +
                "[\"s3:GetReplicationConfiguration\",\"s3:ListBucket\"],\"Effect\": \"Allow\",\"Resource\": " +
                "[\""+ "arn:aws:s3:::" + source + "\"]},{\"Action\": " +
                "[\"s3:GetObjectVersion\",\"s3:GetObjectVersionAcl\"],\"Effect\": \"Allow\",\"Resource\": " +
                "[\"" + "arn:aws:s3:::" + source + "/*" + "\"]},{\"Action\": " +
                "[\"s3:ReplicateObject\",\"s3:ReplicateDelete\"],\"Effect\": \"Allow\",\"Resource\": " +
                "\"arn:aws:s3:::" + destination + "/*" + "\"}]}";

        roleResource.getPolicies().add(policyResource);
        policyResource.setPolicyName("replication-policy");
        policyResource.setPolicyDocument((Map<String, Object>) ObjectUtils.fromJson(policyDocumentString));

        roleResource.create(cloud);

        } catch (Exception error) {
            throw new BeamException("Cannot create bucket replication role: " + error.getMessage());
        }
    }

    private void createReplication(String source, String destination, AmazonS3Client s3Client, AWSCloud cloud) {

        PrintWriter out = new AnsiRenderWriter(System.out, true);
        out.print("\n@|yellow Creating bucket replication from " + source + " to " + destination + " ...|@");
        out.flush();

        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(source,
                        new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));

        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(destination,
                        new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));

        String roleName = source;

        AmazonIdentityManagementClient iamClient = createClient(AmazonIdentityManagementClient.class, cloud.getProvider());

        GetRoleResult getRoleResult = null;

        try {
            getRoleResult = iamClient.getRole(new GetRoleRequest().withRoleName(roleName));

        } catch (NoSuchEntityException error) {
            createReplicationRole(source, destination, roleName, cloud);
            getRoleResult = iamClient.getRole(new GetRoleRequest().withRoleName(roleName));
        }

        String roleArn = getRoleResult.getRole().getArn();

        Map<String, ReplicationRule> replicationRules = new HashMap<>();
        replicationRules.put(
                "replication-rule",
                new ReplicationRule()
                        .withStatus(ReplicationRuleStatus.Enabled)
                        .withDestinationConfig(
                                new ReplicationDestinationConfig().withBucketARN("arn:aws:s3:::" + destination)));

        s3Client.setBucketReplicationConfiguration(new SetBucketReplicationConfigurationRequest()
                .withBucketName(source)
                .withReplicationConfiguration(new BucketReplicationConfiguration()
                        .withRoleARN(roleArn)
                        .withRules(replicationRules)));
    }

    private void copyBucket(String source, String destination, AmazonS3Client s3Client) {

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(source);
        ObjectListing objectListing;

        do {
            objectListing = s3Client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                displayCopyMessage(objectSummary.getKey());
                s3Client.copyObject(source, objectSummary.getKey(), destination, objectSummary.getKey());
            }

            listObjectsRequest.setMarker(objectListing.getNextMarker());

        } while (objectListing.isTruncated());
    }

    private void stopReplication(String source, String destination, AmazonS3Client s3Client) {
        PrintWriter out = new AnsiRenderWriter(System.out, true);
        out.print("\n@|yellow Stopping bucket replication from " + source + " to " + destination + " ...|@");
        out.flush();

        s3Client.deleteBucketReplicationConfiguration(source);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
    }

    private void startReplication(String source, String destination, AmazonS3Client s3Client, AWSCloud cloud) {
        if (s3Client.doesBucketExist(destination)) {
            createReplication(source, destination, s3Client, cloud);

        } else {
            PrintWriter out = new AnsiRenderWriter(System.out, true);
            out.print("\n@|yellow The destination bucket has not been created, skip replication.|@");
            out.flush();
        }
    }
}
