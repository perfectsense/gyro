package beam.core;

import java.util.ArrayList;
import java.util.List;

public class BeamRuntime {

    private static List<BeamConfigLocation> beamConfigLocations = new ArrayList<>();

    public static List<BeamConfigLocation> getBeamConfigLocations() {
        return beamConfigLocations;
    }

    public static void setBeamConfigLocations(List<BeamConfigLocation> beamConfigLocations) {
        BeamRuntime.beamConfigLocations = beamConfigLocations;
    }
}
