package gyro.aws.elbv2;

import gyro.core.diff.ResourceName;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RedirectActionConfig;

@ResourceName(parent = "action", value = "redirect")
public class RedirectActionConfigResource {
    private String host;
    private String path;
    private String port;
    private String protocol;
    private String query;
    private String statusCode;

    public RedirectActionConfigResource() {

    }

    public RedirectActionConfigResource(RedirectActionConfig redirect) {
        setHost(redirect.host());
        setPath(redirect.path());
        setPort(redirect.port());
        setProtocol(redirect.protocol());
        setQuery(redirect.query());
        setStatusCode(redirect.statusCodeAsString());
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public RedirectActionConfig toRedirect() {
        return RedirectActionConfig.builder()
                .host(getHost())
                .path(getPath())
                .port(getPort())
                .protocol(getProtocol())
                .query(getQuery())
                .statusCode(getStatusCode())
                .build();
    }
}
