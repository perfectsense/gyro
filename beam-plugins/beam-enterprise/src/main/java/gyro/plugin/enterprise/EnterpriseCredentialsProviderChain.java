package gyro.plugin.enterprise;

import beam.core.BeamException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.LinkedList;
import java.util.List;

public class EnterpriseCredentialsProviderChain implements AwsCredentialsProvider {

    private static final Log log = LogFactory.getLog(EnterpriseCredentialsProviderChain.class);

    private List<AwsCredentialsProvider> credentialsProviders = new LinkedList<AwsCredentialsProvider>();

    private boolean reuseLastProvider = true;
    private AwsCredentialsProvider lastUsedProvider;

    public EnterpriseCredentialsProviderChain(AwsCredentialsProvider... credentialsProviders) {
        if (credentialsProviders == null || credentialsProviders.length == 0) {
            throw new IllegalArgumentException("No credential providers specified");
        }

        for (AwsCredentialsProvider provider : credentialsProviders) {
            this.credentialsProviders.add(provider);
        }
    }

    public boolean getReuseLastProvider() {
        return reuseLastProvider;
    }

    public void setReuseLastProvider(boolean b) {
        this.reuseLastProvider = b;
    }

    public AwsCredentials resolveCredentials() {
        if (reuseLastProvider && lastUsedProvider != null) {
            return lastUsedProvider.resolveCredentials();
        }

        for (AwsCredentialsProvider provider : credentialsProviders) {
            try {
                AwsCredentials credentials = provider.resolveCredentials();

                if (credentials.accessKeyId() != null && credentials.secretAccessKey() != null && validate(provider)) {
                    log.debug("Loading credentials from " + provider.toString());

                    lastUsedProvider = provider;
                    return credentials;
                }
            } catch (BeamException be) {
                throw be;
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                log.debug("Unable to load credentials from " + provider.toString() + ": " + e.getMessage());
            }
        }

        throw SdkClientException.create("Unable to load AWS credentials from any provider in the chain");
    }

    private boolean validate(AwsCredentialsProvider provider) {
        /*
        AmazonS3Client client = new AmazonS3Client(provider);

        try {
            client.listBuckets();
        } catch (com.amazonaws.AmazonServiceException error) {
            client.setRegion(Region.getRegion(Regions.CN_NORTH_1));
            try {
                client.listBuckets();
            } catch (com.amazonaws.AmazonServiceException e) {
                return false;
            }
        }
        */

        return true;
    }

    public List<AwsCredentialsProvider> getProviders() {
        return credentialsProviders;
    }

}
