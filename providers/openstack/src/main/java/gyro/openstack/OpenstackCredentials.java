package gyro.openstack;

import gyro.core.diff.ResourceName;
import gyro.lang.Credentials;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@ResourceName("credentials")
public class OpenstackCredentials extends Credentials {

    private String userName;

    private String apiKey;

    private String region;

    @Override
    public String getCloudName() {
        return "openstack";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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

        return mapBuilder.build();
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
    }
}
