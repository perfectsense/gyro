package beam.core;

import beam.parser.BeamConfigGenerator;

import java.util.ArrayList;
import java.util.List;

public class BeamRuntime {

    private static List<BeamConfigLocation> beamConfigLocations = new ArrayList<>();
    private static BeamConfigGenerator beamConfigTranslator = new BeamConfigGenerator();

    public static List<BeamConfigLocation> getBeamConfigLocations() {
        return beamConfigLocations;
    }

    public static void setBeamConfigLocations(List<BeamConfigLocation> beamConfigLocations) {
        BeamRuntime.beamConfigLocations = beamConfigLocations;
    }

    public static BeamConfigGenerator getBeamConfigTranslator() {
        return beamConfigTranslator;
    }

    public static void setBeamConfigTranslator(BeamConfigGenerator beamConfigTranslator) {
        BeamRuntime.beamConfigTranslator = beamConfigTranslator;
    }
}
