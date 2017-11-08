package beam.aws;

import beam.BeamException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

public class EnterpriseCredentialsProviderChain implements AWSCredentialsProvider {

    private static final Log log = LogFactory.getLog(EnterpriseCredentialsProviderChain.class);

    private List<AWSCredentialsProvider> credentialsProviders =
            new LinkedList<AWSCredentialsProvider>();

    private boolean reuseLastProvider = true;
    private AWSCredentialsProvider lastUsedProvider;

    public EnterpriseCredentialsProviderChain(AWSCredentialsProvider... credentialsProviders) {
        if (credentialsProviders == null || credentialsProviders.length == 0)
            throw new IllegalArgumentException("No credential providers specified");

        for (AWSCredentialsProvider provider : credentialsProviders) {
            this.credentialsProviders.add(provider);
        }
    }

    public boolean getReuseLastProvider() {
        return reuseLastProvider;
    }

    public void setReuseLastProvider(boolean b) {
        this.reuseLastProvider = b;
    }

    public AWSCredentials getCredentials() {
        if (reuseLastProvider && lastUsedProvider != null) {
            return lastUsedProvider.getCredentials();
        }

        for (AWSCredentialsProvider provider : credentialsProviders) {
            try {
                AWSCredentials credentials = provider.getCredentials();

                if (credentials.getAWSAccessKeyId() != null &&
                        credentials.getAWSSecretKey() != null && validate(provider)) {
                    log.debug("Loading credentials from " + provider.toString());

                    lastUsedProvider = provider;
                    return credentials;
                }
            } catch (BeamException be) {
                throw be;
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                log.debug("Unable to load credentials from " + provider.toString() +
                        ": " + e.getMessage());
            }
        }

        throw new AmazonClientException("Unable to load AWS credentials from any provider in the chain");
    }

    public void refresh() {
        for (AWSCredentialsProvider provider : credentialsProviders) {
            if (provider instanceof ProfileCredentialsProvider) {
                continue;
            }

            provider.refresh();
        }
    }

    private boolean validate(AWSCredentialsProvider provider) {
        AmazonS3Client client = new AmazonS3Client(provider);

        try {
            client.listBuckets();
        } catch (com.amazonaws.AmazonServiceException error) {
            return false;
        }

        return true;
    }

    public List<AWSCredentialsProvider> getProviders() {
        return credentialsProviders;
    }
}
