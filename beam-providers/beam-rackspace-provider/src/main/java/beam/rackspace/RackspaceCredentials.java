package beam.rackspace;

import beam.core.diff.ResourceName;
import beam.lang.Credentials;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@ResourceName("credentials")
public class RackspaceCredentials extends Credentials {

    private String profileName;

    private String region;

    @Override
    public String getCloudName() {
        return "rackspace";
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
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

        mapBuilder.put("accessKeyId", "key");
        mapBuilder.put("secretKey", "secret");

        return mapBuilder.build();
    }

    @Override
    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
    }
}
