package beam.aws.cloudfront;

import beam.core.diff.Diffable;

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

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public Integer getOriginKeepAliveTimeout() {
        return originKeepAliveTimeout;
    }

    public void setOriginKeepAliveTimeout(Integer originKeepAliveTimeout) {
        this.originKeepAliveTimeout = originKeepAliveTimeout;
    }

    public Integer getOriginReadTimeout() {
        return originReadTimeout;
    }

    public void setOriginReadTimeout(Integer originReadTimeout) {
        this.originReadTimeout = originReadTimeout;
    }

    public String getOriginProtocolPolicy() {
        return originProtocolPolicy;
    }

    public void setOriginProtocolPolicy(String originProtocolPolicy) {
        this.originProtocolPolicy = originProtocolPolicy;
    }

    public List<String> getOriginSslProtocols() {
        return originSslProtocols;
    }

    public void setOriginSslProtocols(List<String> originSslProtocols) {
        this.originSslProtocols = originSslProtocols;
    }

    @Override
    public String primaryKey() {
        return "custom-origin";
    }

    @Override
    public String toDisplayString() {
        return "custom origin";
    }
}
