package beam.aws.config;

import beam.BeamException;
import beam.BeamResource;
import beam.aws.AWSCloud;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.google.common.base.Throwables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;

public abstract class AWSResource<A> extends BeamResource<AWSCloud, A> {

    private Region region;

    public <T extends AmazonWebServiceClient> T createClient(Class<T> clientClass, AWSCredentialsProvider provider) {

        try {
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

    @ FunctionalInterface
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
                   System.out.print(" Keep waiting? (Y/n) ");
                   System.out.flush();

                   BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                   try {
                       if ("n".equalsIgnoreCase(confirmReader.readLine())) {
                           throw new BeamException("AWS service request failed!\n" + error.getMessage());
                       } else {
                           counter = 10;
                       }
                   } catch (IOException ioe) {
                       throw Throwables.propagate(ioe);
                   }
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
