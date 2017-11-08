package beam.aws;

import com.amazonaws.auth.BasicSessionCredentials;
import com.psddev.dari.util.ObjectUtils;

import java.util.Map;

public class EnterpriseCredentials extends BasicSessionCredentials {

    private final String userArn;

    private final Long expiration;

    public EnterpriseCredentials(Map<String, Object> credentialsMap) {
        super(
                ObjectUtils.to(String.class, credentialsMap.get("accessKeyId")),
                ObjectUtils.to(String.class, credentialsMap.get("secretAccessKey")),
                ObjectUtils.to(String.class, credentialsMap.get("sessionToken")));

        this.userArn = ObjectUtils.to(String.class, credentialsMap.get("userArn"));
        this.expiration = ObjectUtils.to(Long.class, credentialsMap.get("expiration"));
    }

    public String getUserArn() {
        return userArn;
    }

    public Long getExpiration() {
        return expiration;
    }

}
