package gyro.aws.elbv2;

import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     health-check
 *         health-check-interval: "90"
 *         health-check-path: "/"
 *         health-check-port: "traffic-port"
 *         health-check-protocol: "HTTP"
 *         health-check-timeout: "30"
 *         healthy-threshold: "2"
 *         matcher: "200"
 *         unhealthy-threshold: "2"
 *     end
 */

@ResourceName(parent = "target-group", value = "health-check")
public class HealthCheckResource {

    private Integer healthCheckInterval;
    private String healthCheckPath;
    private String healthCheckPort;
    private String healthCheckProtocol;
    private Integer healthCheckTimeout;
    private Integer healthyThreshold;
    private String matcher;
    private Integer unhealthyThreshold;

    public HealthCheckResource() {

    }

    public HealthCheckResource(TargetGroup targetGroup) {
        setHealthCheckInterval(targetGroup.healthCheckIntervalSeconds());
        setHealthCheckPath(targetGroup.healthCheckPath());
        setHealthCheckPort(targetGroup.healthCheckPort());
        setHealthCheckProtocol(targetGroup.healthCheckProtocolAsString());
        setHealthCheckTimeout(targetGroup.healthCheckTimeoutSeconds());
        setHealthyThreshold(targetGroup.healthyThresholdCount());
        if (targetGroup.matcher() != null) {
            setMatcher(targetGroup.matcher().httpCode());
        }
        setUnhealthyThreshold(targetGroup.unhealthyThresholdCount());
    }

    /**
     *  The approximate amount of time between health checks of a target (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    /**
     *  The ping path destination on targets for health checks (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public void setHealthCheckPath(String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    /**
     *  The port used when an alb performs health checks on targets (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckPort() {
        return healthCheckPort;
    }

    public void setHealthCheckPort(String healthCheckPort) {
        this.healthCheckPort = healthCheckPort;
    }

    /**
     *  The port used when an alb performs health checks on targets (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckProtocol() {
        return healthCheckProtocol;
    }

    public void setHealthCheckProtocol(String healthCheckProtocol) {
        this.healthCheckProtocol = healthCheckProtocol;
    }

    /**
     *  The amount of time, in seconds, an unresponsive target means a failed health check (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHealthCheckTimeout() {
        return healthCheckTimeout;
    }

    public void setHealthCheckTimeout(Integer healthCheckTimeout) {
        this.healthCheckTimeout = healthCheckTimeout;
    }

    /**
     *  Health check successes required for an unhealthy target to be considered healthy (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    /**
     *  HTTP code that signals a successful response from a target (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    /**
     *  Health check failures required by an unhealthy target to be considered unhealthy (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }
}
