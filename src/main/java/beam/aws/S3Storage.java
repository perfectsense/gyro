package beam.aws;

import java.io.IOException;
import java.io.InputStream;

import beam.BeamException;
import beam.BeamStorage;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

public class S3Storage extends BeamStorage {

    private final AWSCloud cloud;
    private final String bucket;

    public S3Storage(AWSCloud cloud, String bucket) {
        this.cloud = cloud;
        this.bucket = bucket;
    }

    @Override
    public InputStream get(String path, String region) {
        AmazonS3Client client = new AmazonS3Client(cloud.getProvider());

        if (region != null && client.doesBucketExist(bucket)) {
            String bucketLocation = client.getBucketLocation(bucket);
            Region bucketRegion = com.amazonaws.services.s3.model.Region.fromValue(bucketLocation).toAWSRegion();

            client.setRegion(bucketRegion);
        }

        try {
            S3Object object = client.getObject(bucket, path);

            return object.getObjectContent();

        } catch (AmazonS3Exception error) {
            String errorCode = error.getErrorCode();

            if ("NoSuchBucket".equals(errorCode) ||
                    "NoSuchKey".equals(errorCode) ||
                    "AccessDenied".equals(errorCode)) {
                return null;

            } else {
                throw error;
            }
        }
    }

    @Override
    public InputStream get(String path) throws IOException {
        return get(path, null);
    }

    @Override
    public void put(String region, String path, InputStream content, String contentType, long length) {
        AmazonS3Client client = new AmazonS3Client(cloud.getProvider());

        if (region != null && client.doesBucketExist(bucket)) {
            String bucketLocation = client.getBucketLocation(bucket);
            Region bucketRegion = com.amazonaws.services.s3.model.Region.fromValue(bucketLocation).toAWSRegion();

            client.setRegion(bucketRegion);
        }

        if (content != null) {
            if (!client.doesBucketExist(bucket)) {
                client.createBucket(bucket);
            }

            ObjectMetadata s3Metadata = new ObjectMetadata();

            if (contentType != null) {
                s3Metadata.setContentType("text/plain;charset=UTF-8");
            }

            s3Metadata.setContentLength(length);
            try {
                client.putObject(bucket, path, content, s3Metadata);
            } catch(AmazonS3Exception error) {
                throw new BeamException(String.format("Unable to save to S3 %s:%s", bucket, path), error);
            }

        } else if (client.doesBucketExist(bucket)) {
            client.deleteObject(bucket, path);
        }
    }

    public String getBucket() {
        return bucket;
    }

    @Override
    public boolean doesExist(String region) {
        AmazonS3Client client = new AmazonS3Client(cloud.getProvider());

        if (region != null) {
            client.setRegion(RegionUtils.getRegion(region.toLowerCase()));
        }

        return client.doesBucketExist(bucket);
    }

}