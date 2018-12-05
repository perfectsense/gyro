package beam.aws;

import beam.core.BeamCredentials;
import beam.core.diff.ResourceName;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.beanutils.BeanUtils;
import org.joda.time.DateTime;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@ResourceName("credentials")
public class AwsBeamCredentials extends BeamCredentials {

    private AwsCredentialsProvider provider;

    private String profileName;

    private String region;

    public AwsBeamCredentials() {
        this.provider = AwsCredentialsProviderChain.builder()
                .credentialsProviders(DefaultCredentialsProvider.create())
                .build();
    }

    public AwsCredentialsProvider getProvider() {
        return provider;
    }

    @Override
    public String getName() {
        return "aws";
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;

        this.provider = AwsCredentialsProviderChain.builder()
                .credentialsProviders(
                        ProfileCredentialsProvider.create(profileName),
                        DefaultCredentialsProvider.create()
                )
                .build();
    }

    public void setProvider(AwsCredentialsProvider provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        AwsCredentials creds;

        AwsCredentialsProvider provider = getProvider();
        creds = provider.resolveCredentials();

        mapBuilder.put("accessKeyId", creds.accessKeyId());
        mapBuilder.put("secretKey", creds.secretAccessKey());

        if (creds instanceof AwsSessionCredentials) {
            mapBuilder.put("sessionToken", ((AwsSessionCredentials) creds).sessionToken());
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
