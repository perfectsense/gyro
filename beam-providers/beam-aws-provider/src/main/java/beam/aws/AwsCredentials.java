package beam.aws;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamResolvable;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.beanutils.BeanUtils;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class AwsCredentials extends BeamCredentials {

    private AWSCredentialsProvider provider;

    private String profileName;

    public AwsCredentials() {
        this.provider = new AWSCredentialsProviderChain(DefaultAWSCredentialsProviderChain.getInstance());
    }

    public AWSCredentialsProvider getProvider() {
        return provider;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;

        this.provider = new AWSCredentialsProviderChain(
                new ProfileCredentialsProvider(profileName),
                DefaultAWSCredentialsProviderChain.getInstance()
        );
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

    @Override
    public boolean resolve(BeamConfig config) {
        boolean progress = super.resolve(config);

        if (!progress) {
            return progress;
        }

        for (BeamConfigKey key : getContext().keySet()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getContext().get(key);
            Object value = referable.getValue();

            try {
                BeanUtils.setProperty(this, key.getId(), value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {

            }
        }

        return progress;
    }

}
