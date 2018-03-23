package beam.core;

public abstract class BeamObject {

    private BeamConfigLocation configLocation;

    public BeamConfigLocation getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(BeamConfigLocation configLocation) {
        this.configLocation = configLocation;
    }
}
