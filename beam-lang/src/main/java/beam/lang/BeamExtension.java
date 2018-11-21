package beam.lang;

import beam.parser.antlr4.BeamParser;

public interface BeamExtension {

    String getName();

    BeamConfig applyExtension(BeamParser.ExtensionContext ctx);
}
