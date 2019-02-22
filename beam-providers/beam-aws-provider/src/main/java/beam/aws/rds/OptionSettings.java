package beam.aws.rds;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;

public class OptionSettings extends Diffable {

    private String name;
    private String value;

    /**
     * The name of the option that has settings that you can set.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The current value of the option setting.
     */
    @ResourceDiffProperty(updatable = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String primaryKey() {
        return getName();
    }

    @Override
    public String toDisplayString() {
        return "option setting " + getName();
    }
}
