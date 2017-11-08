package beam.aws.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Throwables;

public class S3ObjectResource extends AWSResource<S3Object> {

    private static final String OBJECT_CONTENT_URL_KEY = "beam-object-content-url";

    private BeamReference bucket;
    private String key;
    private String objectContentUrl;
    private String etag;

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

    public BeamReference getBucket() {
        return newParentReference(BucketResource.class, bucket);
    }

    public void setBucket(BeamReference bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    @ResourceDiffProperty(updatable = true)
    public String getObjectContentUrl() {
        return objectContentUrl;
    }

    public void setObjectContentUrl(String objectContentUrl) {
        this.objectContentUrl = objectContentUrl;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getBucket(), getKey());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, S3Object s3Object) {
        setBucket(newReference(BucketResource.class, s3Object.getBucketName()));
        setKey(s3Object.getKey());
        setObjectContentUrl(s3Object.getObjectMetadata().getUserMetaDataOf(OBJECT_CONTENT_URL_KEY));
        setEtag(s3Object.getObjectMetadata().getETag());
    }

    @Override
    public BeamResource<AWSCloud, S3Object> findCurrent(AWSCloud cloud, BeamResourceFilter filter) {
        AmazonS3Client s3Client = createClient(AmazonS3Client.class, cloud.getProvider());

        try {
            S3Object s3Object = s3Client.getObject(getBucket().awsId(), getKey());
            S3ObjectResource current = new S3ObjectResource();

            current.init(cloud, filter, s3Object);
            return current;

        } catch (AmazonS3Exception error) {
            if (error.getStatusCode() == 404) {
                return null;

            } else {
                throw error;
            }
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonS3Client s3Client = createClient(AmazonS3Client.class, cloud.getProvider());
        String bucketName = getBucket().awsId();
        String key = getKey();
        String contentUrl = getObjectContentUrl();
        ObjectMetadata metadata = new ObjectMetadata();

        metadata.addUserMetadata(OBJECT_CONTENT_URL_KEY, contentUrl);

        if (contentUrl.startsWith("s3://")) {
            contentUrl = contentUrl.substring(5);
            int slashAt = contentUrl.indexOf('/');

            CopyObjectRequest coRequest = new CopyObjectRequest(
                    contentUrl.substring(0, slashAt),
                    contentUrl.substring(slashAt + 1),
                    bucketName,
                    key);

            coRequest.setNewObjectMetadata(metadata);

            try {
                s3Client.copyObject(coRequest);
            } catch (AmazonS3Exception ase) {
                if (ase.getStatusCode() == 404) {
                    throw new BeamException("The file 's3://" + contentUrl + "' was not found");
                }
            }
        } else {
            try (InputStream contentStream = new URL(contentUrl).openStream()) {
                s3Client.putObject(bucketName, key, contentStream, metadata);

            } catch (IOException error) {
                throw Throwables.propagate(error);
            }
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, S3Object> current, Set<String> changedProperties) {
        create(cloud);
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
        return "s3 object " + getKey();
    }
}
