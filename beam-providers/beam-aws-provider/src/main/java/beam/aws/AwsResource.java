package beam.aws;

import beam.core.BeamException;
import beam.lang.Resource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

import java.lang.reflect.Method;
import java.net.URI;

public abstract class AwsResource extends Resource {

    private SdkClient client;

    protected <T extends SdkClient> T createClient(Class<T> clientClass) {
        return createClient(clientClass, null, null);
    }

    protected <T extends SdkClient> T createClient(Class<T> clientClass, String region, String endpoint) {
        if (client != null) {
            return (T) client;
        }

        try {
            AwsCredentials credentials = (AwsCredentials) resourceCredentials();
            if (credentials == null) {
                throw new BeamException("No credentials associated with the resource.");
            }

            AwsCredentialsProvider provider = credentials.provider();

            Method method = clientClass.getMethod("builder");
            AwsDefaultClientBuilder builder = (AwsDefaultClientBuilder) method.invoke(null);
            builder.credentialsProvider(provider);
            builder.region(Region.of(region != null ? region : credentials.getRegion()));
            builder.httpClientBuilder(ApacheHttpClient.builder());

            if (endpoint != null) {
                builder.endpointOverride(URI.create(endpoint));
            }

            client = (T) builder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return (T) client;
    }

    public Class resourceCredentialsClass() {
        return AwsCredentials.class;
    }

    @FunctionalInterface
    protected interface Service {
        Object apply();
    }

    public Object executeService(Service service) {
        boolean available = false;
        int counter = 10;
        Object result = null;
        while (!available) {
            available = true;
            try {
                result = service.apply();
            } catch (Exception error) {
                available = false;
                counter--;

                if (counter < 0) {
                    throw new BeamException("AWS service request failed!\n" + error.getMessage());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }

        return result;
    }

}
