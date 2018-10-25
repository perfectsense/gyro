package beam.core;

import beam.parser.BeamConfigGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeamRuntime {

    private static List<BeamConfigLocation> beamConfigLocations = new ArrayList<>();

    private static BeamConfigGenerator beamConfigTranslator = new BeamConfigGenerator();

    private static Map<String, BeamContext> contexts = new ConcurrentHashMap<>();

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

    public static Map<String, BeamContext> getContexts() {
        return contexts;
    }

    public static void setContexts(Map<String, BeamContext> context) {
        BeamRuntime.contexts = contexts;
    }
}
