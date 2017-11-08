package beam.aws.config;

import beam.config.Config;

public class AWSLoadBalancerHealthCheckConfig extends Config {

    private String protocol;
    private int port;
    private String path;
    private int timeout = 5;
    private int interval = 10;
    private int unhealthyCount = 2;
    private int healthyCount = 5;

    public String getProtocol() {
        return protocol;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getUnhealthyCount() {
        return unhealthyCount;
    }

    public void setUnhealthyCount(int unhealthyCount) {
        this.unhealthyCount = unhealthyCount;
    }

    public int getHealthyCount() {
        return healthyCount;
    }

    public void setHealthyCount(int healthyCount) {
        this.healthyCount = healthyCount;
    }
}
