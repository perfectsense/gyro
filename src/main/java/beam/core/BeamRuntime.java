package beam.core;

import beam.parser.BeamConfigGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeamRuntime {

    private static List<BeamConfigLocation> beamConfigLocations = new ArrayList<>();

    private static BeamConfigGenerator beamConfigTranslator = new BeamConfigGenerator();

    private static Map<String, Object> context = new ConcurrentHashMap<>();

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

    public static Map<String, Object> getContext() {
        return context;
    }

    public static void setContext(Map<String, Object> context) {
        BeamRuntime.context = context;
    }
}
