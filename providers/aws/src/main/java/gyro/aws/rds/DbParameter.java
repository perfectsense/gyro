package gyro.aws.rds;

import gyro.core.diff.Diffable;
import gyro.core.diff.ResourceDiffProperty;

public class DbParameter extends Diffable {

    private String name;
    private String value;
    private String applyMethod;

    /**
     * The name of the DB parameter. (Required)
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The value of the DB parameter. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * The timing to apply parameter updates. Valid values are ``immediate`` (default) or ``pending-reboot``.
     */
    public String getApplyMethod() {
        if (applyMethod == null) {
            applyMethod = "immediate";
        }

        return applyMethod;
    }

    public void setApplyMethod(String applyMethod) {
        this.applyMethod = applyMethod;
    }

    @Override
    public String primaryKey() {
        return getName();
    }

    @Override
    public String toDisplayString() {
        return "db parameter " + getName();
    }
}
