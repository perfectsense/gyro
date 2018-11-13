package beam.lang;

import beam.parser.antlr4.BeamParser;

public interface BeamExtension {

    String getName();

    void enterExtension(BeamParser.ExtensionContext ctx, BeamConfig context);

    void exitExtension(BeamParser.ExtensionContext ctx, BeamConfig context);
}
