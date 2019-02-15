package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.cloudfront.model.CustomOriginConfig;

import java.util.Arrays;
import java.util.List;

public class CloudFrontCustomOrigin extends Diffable {

    private Integer httpPort;
    private Integer httpsPort;
    private Integer originKeepAliveTimeout;
    private Integer originReadTimeout;
    private String originProtocolPolicy;
    private List<String> originSslProtocols;

    public CloudFrontCustomOrigin() {
        setHttpPort(80);
        setHttpsPort(443);
        setOriginKeepAliveTimeout(5);
        setOriginReadTimeout(30);
        setOriginProtocolPolicy("http-only");
        setOriginSslProtocols(Arrays.asList("TLSv1", "TLSv1.1", "TLSv1.2"));
    }

    public CloudFrontCustomOrigin(CustomOriginConfig originConfig) {
        setHttpPort(originConfig.httpPort());
        setHttpsPort(originConfig.httpsPort());
        setOriginKeepAliveTimeout(originConfig.originKeepaliveTimeout());
        setOriginProtocolPolicy(originConfig.originProtocolPolicyAsString());
        setOriginReadTimeout(originConfig.originReadTimeout());
        setOriginSslProtocols(originConfig.originSslProtocols().itemsAsStrings());
    }

    /**
     * The port the origin listens for http.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * The port the origin listens for https.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    /**
     * The amount of time to keep an idle connection to the origin.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getOriginKeepAliveTimeout() {
        return originKeepAliveTimeout;
    }

    public void setOriginKeepAliveTimeout(Integer originKeepAliveTimeout) {
        this.originKeepAliveTimeout = originKeepAliveTimeout;
    }

    /**
     * The max amount of a time CloudFront will wait, in seconds, for an initial connection, and subsequent reads. Valid values are between 4 and 60.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getOriginReadTimeout() {
        return originReadTimeout;
    }

    public void setOriginReadTimeout(Integer originReadTimeout) {
        this.originReadTimeout = originReadTimeout;
    }

    /**
     * The protocol CloudFront should use to connect to the origin. Valid values are ``http-only``, ``https-only``, or ``match-viewer``.
     */
    @ResourceDiffProperty(updatable = true)
    public String getOriginProtocolPolicy() {
        return originProtocolPolicy;
    }

    public void setOriginProtocolPolicy(String originProtocolPolicy) {
        this.originProtocolPolicy = originProtocolPolicy;
    }

    /**
     * SSL protocols CloudFront is allow to connect to the origin with. Valid values are ``SSLv3``, ``TLSv1``, ``TLSv1.1``, ``TLSv1.2``.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getOriginSslProtocols() {
        return originSslProtocols;
    }

    public void setOriginSslProtocols(List<String> originSslProtocols) {
        this.originSslProtocols = originSslProtocols;
    }

    public CustomOriginConfig toCustomOriginConfig() {
        return CustomOriginConfig.builder()
            .httpPort(getHttpPort())
            .httpsPort(getHttpsPort())
            .originKeepaliveTimeout(getOriginKeepAliveTimeout())
            .originProtocolPolicy(getOriginProtocolPolicy())
            .originReadTimeout(getOriginReadTimeout())
            .originSslProtocols(o -> o.itemsWithStrings(getOriginSslProtocols()).quantity(getOriginSslProtocols().size()))
            .build();
    }

    @Override
    public String primaryKey() {
        return "custom-origin";
    }

    @Override
    public String toDisplayString() {
        return "custom origin config";
    }
}
