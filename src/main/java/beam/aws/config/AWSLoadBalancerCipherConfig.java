package beam.aws.config;

import beam.config.Config;

import java.util.ArrayList;
import java.util.List;

public class AWSLoadBalancerCipherConfig extends Config {
    private String predefinedPolicy;
    private String serverOrderPreference;
    private List<String> sslProtocols;
    private List<String> sslCiphers;

    public String getPredefinedPolicy() {
        return predefinedPolicy;
    }

    public void setPredefinedPolicy(String predefinedPolicy) {
        this.predefinedPolicy = predefinedPolicy;
    }

    public String getServerOrderPreference() {
        return serverOrderPreference;
    }

    public void setServerOrderPreference(String serverOrderPreference) {
        this.serverOrderPreference = serverOrderPreference;
    }

    public List<String> getSslProtocols() {
        if (sslProtocols == null) {
            sslProtocols = new ArrayList<>();
        }
        return sslProtocols;
    }

    public void setSslProtocols(List<String> sslProtocols) {
        this.sslProtocols = sslProtocols;
    }

    public List<String> getSslCiphers() {
        if (sslCiphers == null) {
            sslCiphers = new ArrayList<>();
        }
        return sslCiphers;
    }

    public void setSslCiphers(List<String> sslCiphers) {
        this.sslCiphers = sslCiphers;
    }
}
