package beam.aws.resources;

import beam.aws.AwsCredentials;
import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;

import java.lang.reflect.Constructor;

public abstract class AwsResource extends BeamResource {

    private Region region;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass) {

        try {
            AwsCredentials credentials = (AwsCredentials) getResourceCredentials();
            AWSCredentialsProvider provider = credentials.getProvider();
            Constructor<?> constructor = clientClass.getConstructor(AWSCredentialsProvider.class);

            T client = (T) constructor.newInstance(provider);
            if (getRegion() != null) {
                client.setRegion(getRegion());
            }

            return client;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public Class getResourceCredentialsClass() {
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

}
