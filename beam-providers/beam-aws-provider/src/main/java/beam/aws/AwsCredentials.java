package beam.aws;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.google.common.base.CaseFormat;
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
    public boolean resolve(BeamContext context) {
        boolean progress = super.resolve(context);

        for (BeamContextKey key : listContextKeys()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getReferable(key);
            Object value = referable.getValue();

            try {
                String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key.getId());
                BeanUtils.setProperty(this, keyId, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {

            }
        }

        return progress;
    }

}
