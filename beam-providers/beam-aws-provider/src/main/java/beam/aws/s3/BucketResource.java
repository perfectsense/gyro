package beam.aws.s3;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.CompactMap;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.Payer;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ResourceName("bucket")
public class BucketResource extends AwsResource {

    private String name;

    private Boolean enableObjectLock;

    private Map<String, String> tags;

    private Boolean enableAccelerateConfig;

    private Boolean enableVersion;

    private Boolean enablePay;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnableObjectLock() {
        if (enableObjectLock == null) {
            enableObjectLock = false;
        }
        return enableObjectLock;
    }

    public void setEnableObjectLock(Boolean enableObjectLock) {
        this.enableObjectLock = enableObjectLock;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableAccelerateConfig() {
        if (enableAccelerateConfig == null) {
            enableAccelerateConfig = false;
        }
        return enableAccelerateConfig;
    }

    public void setEnableAccelerateConfig(Boolean enableAccelerateConfig) {
        this.enableAccelerateConfig = enableAccelerateConfig;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableVersion() {
        if (enableVersion == null) {
            enableVersion = false;
        }
        return enableVersion;
    }

    public void setEnableVersion(Boolean enableVersion) {
        this.enableVersion = enableVersion;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnablePay() {
        if (enablePay == null) {
            enablePay = false;
        }
        return enablePay;
    }

    public void setEnablePay(Boolean enablePay) {
        this.enablePay = enablePay;
    }

    @Override
    public void refresh() {
        S3Client client = createClient(S3Client.class);

        ListBucketsResponse listBucketsResponse = client.listBuckets(
                lbr -> lbr.build()
        );

        Bucket bucket = null;
        for(Bucket bucketObj : listBucketsResponse.buckets()) {
            if (bucketObj.name().equals(getName())) {
                bucket = bucketObj;
            }
        }

        if (bucket != null) {
            loadTags(client);

            loadAccelerateConfig(client);

            loadEnableVersion(client);

            loadEnablePay(client);

        } else {
            throw new BeamException(MessageFormat.format("Bucket - {0} not found.", getName()));
        }
    }

    @Override
    public void create() {
        S3Client client = createClient(S3Client.class);
        client.createBucket(
                cb->cb.bucket(getName())
                        .objectLockEnabledForBucket(getEnableObjectLock())
                        .build()
        );

        if (!getTags().isEmpty()) {
            saveTags(client);
        }

        if (getEnableAccelerateConfig()) {
            saveAccelerateConfig(client);
        }

        if (getEnableVersion()) {
            saveEnableVersion(client);
        }

        if(getEnablePay()) {
            saveEnablePay(client);
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        S3Client client = createClient(S3Client.class);

        if (changedProperties.contains("tags")) {
            saveTags(client);
        }

        if (changedProperties.contains("enableAccelerateConfig")) {
            saveAccelerateConfig(client);
        }

        if (changedProperties.contains("enableVersion")) {
            saveEnableVersion(client);
        }

        if(changedProperties.contains("enablePay")) {
            saveEnablePay(client);
        }
    }

    @Override
    public void delete() {
        S3Client client = createClient(S3Client.class);
        client.deleteBucket(
                db->db.bucket(getName())
                        .build()
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (!getName().isEmpty()) {
            sb.append(getName());
        } else {
            sb.append("Bucket");
        }

        return sb.toString();
    }

    private void loadTags(S3Client client) {
        try {
            GetBucketTaggingResponse bucketTagging = client.getBucketTagging(
                    gbt -> gbt.bucket(getName())
            );

            for(Tag tag : bucketTagging.tagSet()) {
                getTags().put(tag.key(),tag.value());
            }

        } catch (S3Exception s3ex) {
            if (s3ex.awsErrorDetails().errorCode().equals("NoSuchTagSet")) {
                getTags().clear();
            } else {
                throw s3ex;
            }
        }
    }

    private void saveTags(S3Client client) {
        if (getTags().isEmpty()) {
            client.deleteBucketTagging(
                    dbt->dbt.bucket(getName())
                            .build()
            );
        } else {
            Set<Tag> tagSet = new HashSet<>();
            for (String key : getTags().keySet()) {
                tagSet.add(Tag.builder().key(key).value(getTags().get(key)).build());
            }

            client.putBucketTagging(
                    pbt -> pbt.bucket(getName())
                            .tagging(
                                    t -> t.tagSet(tagSet)
                            )
                            .build()
            );
        }
    }

    private void loadAccelerateConfig(S3Client client) {
        GetBucketAccelerateConfigurationResponse bucketAccelerateConfigurationResponse = client.getBucketAccelerateConfiguration(
                bac -> bac.bucket(getName())
                        .build()
        );

        setEnableAccelerateConfig(bucketAccelerateConfigurationResponse.status().equals(BucketAccelerateStatus.ENABLED));
    }

    private void saveAccelerateConfig(S3Client client) {
        client.putBucketAccelerateConfiguration(
                bac->bac.bucket(getName())
                        .accelerateConfiguration(
                                ac->ac.status(getAccelerateStatus())
                        )
                        .build()
        );
    }

    private BucketAccelerateStatus getAccelerateStatus() {
        if (getEnableAccelerateConfig()) {
            return BucketAccelerateStatus.ENABLED;
        } else {
            return BucketAccelerateStatus.SUSPENDED;
        }
    }

    private void loadEnableVersion(S3Client client) {
        GetBucketVersioningResponse bucketVersioningResponse = client.getBucketVersioning(
                bv -> bv.bucket(getName())
                        .build()
        );

        setEnableVersion(bucketVersioningResponse.status().equals(BucketVersioningStatus.ENABLED));
    }

    private void saveEnableVersion(S3Client client) {
        client.putBucketVersioning(
                bv->bv.bucket(getName())
                        .versioningConfiguration(
                                vc->vc.status(getVersionStatus())
                        )
                        .build()
        );

        // Todo
        //mfa delete
    }

    private BucketVersioningStatus getVersionStatus() {
        if (getEnableVersion()) {
            return BucketVersioningStatus.ENABLED;
        } else {
            return BucketVersioningStatus.SUSPENDED;
        }
    }

    private void loadEnablePay(S3Client client) {
        GetBucketRequestPaymentResponse requestPaymentResponse = client.getBucketRequestPayment(
                brp -> brp.bucket(getName())
                        .build()
        );

        setEnablePay(requestPaymentResponse.payer().equals(Payer.REQUESTER));
    }

    private void saveEnablePay(S3Client client) {
        client.putBucketRequestPayment(
                brp->brp.bucket(getName())
                        .requestPaymentConfiguration(
                                pc->pc.payer(getPaymentStatus())
                        )
                        .build()
        );
    }

    private Payer getPaymentStatus() {
        if (getEnablePay()) {
            return Payer.REQUESTER;
        } else {
            return Payer.BUCKET_OWNER;
        }
    }
}
