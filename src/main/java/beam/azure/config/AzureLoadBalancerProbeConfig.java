package beam.azure.config;

import beam.config.Config;

public class AzureLoadBalancerProbeConfig extends Config {
    private String protocol;
    private int port;
    private String path;
    private int interval = 10;
    private int numberOfProbes = 2;

    public String getProtocol() {
        if (protocol == null) {
            protocol = "HTTP";
        }

        return protocol.toUpperCase();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getNumberOfProbes() {
        return numberOfProbes;
    }

    public void setNumberOfProbes(int numberOfProbes) {
        this.numberOfProbes = numberOfProbes;
    }
}
