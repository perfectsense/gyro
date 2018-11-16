package beam.aws;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Map;

public class AwsCredentials extends BeamCredentials {

    private final AWSCredentialsProvider provider;

    private String profileName;

    public AwsCredentials() {
        this.provider = null;
    }

    public AWSCredentialsProvider getProvider() {
        return provider;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String getName() {
        return "aws";
    }

    @Override
    public void saveState(BeamResource resource) {
        getStateBackend().saveState(this, resource.getResourceIdentifier(), resource);
    }

    @Override
    public void deleteState(BeamResource resource) {
        getStateBackend().deleteState(this, resource.getResourceIdentifier());
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        AWSCredentials creds;

        try {
            AWSCredentialsProvider provider = getProvider();
            if (refresh) {
                provider.refresh();
            }

            creds = provider.getCredentials();
        } catch (AmazonClientException ace) {
            throw new BeamException(ace.getMessage(), null, "no-account");
        }

        mapBuilder.put("accessKeyId", creds.getAWSAccessKeyId());
        mapBuilder.put("secretKey", creds.getAWSSecretKey());

        if (creds instanceof AWSSessionCredentials) {
            mapBuilder.put("sessionToken", ((AWSSessionCredentials) creds).getSessionToken());
        }

        Long expiration = DateTime.now().plusDays(1).getMillis();
        mapBuilder.put("expiration", Long.toString(expiration));

        return mapBuilder.build();
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
    }

}
