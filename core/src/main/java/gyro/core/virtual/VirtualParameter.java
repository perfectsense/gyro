package gyro.core.virtual;

import gyro.core.GyroException;
import gyro.core.resource.Scope;

public class VirtualParameter {

    private final String name;

    public VirtualParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void copy(Scope source, Scope destination) {
        if (!source.containsKey(name)) {
            throw new GyroException(String.format(
                "[%s] parameter is required!",
                name));

        } else {
            destination.put(name, source.get(name));
        }
    }

}
