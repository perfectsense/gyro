package gyro.core.resource;

import gyro.core.scope.Settings;

public class SelfSettings extends Settings {

    private Resource self;

    public Resource getSelf() {
        return self;
    }

    public void setSelf(Resource self) {
        this.self = self;
    }

}
