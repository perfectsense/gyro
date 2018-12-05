package beam.aws.resources;

import beam.aws.AwsBeamCredentials;
import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import beam.lang.BCL;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

import java.lang.reflect.Method;
import java.time.Duration;

public abstract class AwsResource extends BeamResource {

    private Region region;

    public <T extends SdkClient> T createClient(Class<T> clientClass) {

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
            builder.httpClient(
                    ApacheHttpClient.builder()
                            .connectionAcquisitionTimeout(Duration.ofSeconds(10000))
                            .connectionTimeout(Duration.ofSeconds(100000))
                            .build()
            );

            if (getRegion() != null) {
                builder.region(getRegion());
            }

            return (T) builder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setRegion(String region) {
        this.region = Region.of(region);
    }

}
