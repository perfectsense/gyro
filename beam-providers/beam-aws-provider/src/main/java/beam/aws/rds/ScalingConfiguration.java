package beam.aws.rds;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;

public class ScalingConfiguration extends Diffable {

    private Boolean autoPause;
    private Integer maxCapacity;
    private Integer minCapacity;
    private Integer secondsUntilAutoPause;

    /**
     * A value that specifies whether to allow or disallow automatic pause for an Aurora DB cluster in serverless DB engine mode. A DB cluster can be paused only when it's idle (it has no connections).
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getAutoPause() {
        return autoPause;
    }

    public void setAutoPause(Boolean autoPause) {
        this.autoPause = autoPause;
    }

    /**
     * The maximum capacity for an Aurora DB cluster in serverless DB engine mode. Valid capacity values are ``2``, ``4``, ``8``, ``16``, ``32``, ``64``, ``128``, and ``256``. The maximum capacity must be greater than or equal to the minimum capacity.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * The minimum capacity for an Aurora DB cluster in serverless DB engine mode. Valid capacity values are ``2``, ``4``, ``8``, ``16``, ``32``, ``64``, ``128``, and ``256``. The minimum capacity must be less than or equal to the maximum capacity.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getMinCapacity() {
        return minCapacity;
    }

    public void setMinCapacity(Integer minCapacity) {
        this.minCapacity = minCapacity;
    }

    /**
     * The time, in seconds, before an Aurora DB cluster in serverless mode is paused.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getSecondsUntilAutoPause() {
        return secondsUntilAutoPause;
    }

    public void setSecondsUntilAutoPause(Integer secondsUntilAutoPause) {
        this.secondsUntilAutoPause = secondsUntilAutoPause;
    }

    @Override
    public String primaryKey() {
        return "scaling configuration";
    }

    @Override
    public String toDisplayString() {
        return "scaling configuration";
    }
}
