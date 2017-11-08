package beam.aws;

import java.io.IOException;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.StringUtils;

public class GatewayCredentialsProvider implements AWSCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayCredentialsProvider.class);

    private final InetAddress address;

    private Lazy<AWSCredentials> credentials = new Lazy<AWSCredentials>() {

        @Override
        protected AWSCredentials create() {
            try {
                String apiUrl = StringUtils.addQueryParameters(
                        "http://" + address.getHostAddress() + ":8601/v1/aws-credentials",
                        "user", System.getProperty("user.name"));

                JSONObject response = new Resty(Resty.Option.timeout(2000)).
                        json(apiUrl).
                        object();

                LOGGER.debug("Using AWS credentials from [{}]", apiUrl);
                return new BasicSessionCredentials(
                        response.getString("accessKey"),
                        response.getString("secretKey"),
                        response.getString("sessionToken"));

            } catch (IOException | JSONException error) {
                throw new IllegalStateException(error);
            }
        }
    };

    public GatewayCredentialsProvider(InetAddress address) {
        this.address = address;
    }

    @Override
    public AWSCredentials getCredentials() {
        return credentials.get();
    }

    @Override
    public void refresh() {
        credentials.reset();
    }
}
