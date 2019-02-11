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

    @ResourceDiffProperty(updatable = true)
    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getOriginKeepAliveTimeout() {
        return originKeepAliveTimeout;
    }

    public void setOriginKeepAliveTimeout(Integer originKeepAliveTimeout) {
        this.originKeepAliveTimeout = originKeepAliveTimeout;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getOriginReadTimeout() {
        return originReadTimeout;
    }

    public void setOriginReadTimeout(Integer originReadTimeout) {
        this.originReadTimeout = originReadTimeout;
    }

    @ResourceDiffProperty(updatable = true)
    public String getOriginProtocolPolicy() {
        return originProtocolPolicy;
    }

    public void setOriginProtocolPolicy(String originProtocolPolicy) {
        this.originProtocolPolicy = originProtocolPolicy;
    }

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
