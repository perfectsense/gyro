package beam.aws;

import beam.core.BeamException;
import beam.core.BeamResource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

import java.lang.reflect.Method;

public abstract class AwsResource extends BeamResource {

    private SdkClient client;

    protected <T extends SdkClient> T createClient(Class<T> clientClass) {
        if (client != null) {
            return (T) client;
        }

        try {
            AwsBeamCredentials credentials = (AwsBeamCredentials) getResourceCredentials();
            if (credentials == null) {
                throw new BeamException("No credentials associated with the resource.");
            }

            AwsCredentialsProvider provider = credentials.getProvider();

            Method method = clientClass.getMethod("builder");
            AwsDefaultClientBuilder builder = (AwsDefaultClientBuilder) method.invoke(null);
            builder.credentialsProvider(provider);
            builder.region(Region.of(credentials.getRegion()));
            builder.httpClientBuilder(ApacheHttpClient.builder());

            client = (T) builder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return (T) client;
    }

    public Class getResourceCredentialsClass() {
        return AwsBeamCredentials.class;
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
                   /*if (Beam.ui().readBoolean(Boolean.TRUE, " Keep waiting?")) {
                       counter = 10;
                   } else {
                       throw new BeamException("AWS service request failed!\n" + error.getMessage());
                   }
                   */
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
