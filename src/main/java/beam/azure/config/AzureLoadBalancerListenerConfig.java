package beam.azure.config;

import beam.config.Config;

public class AzureLoadBalancerListenerConfig extends Config {

    private String protocol;
    private int sourcePort;
    private String destProtocol;
    private int destPort;
    private String sslCertificateName;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public String getDestProtocol() {
        if (destProtocol == null) {
            setDestProtocol(protocol);
        }

        return destProtocol;
    }

    public void setDestProtocol(String destProtocol) {
        this.destProtocol = destProtocol;
    }

    public String getSslCertificateName() {
        return sslCertificateName;
    }

    public void setSslCertificateName(String sslCertificateName) {
        this.sslCertificateName = sslCertificateName;
    }
}
