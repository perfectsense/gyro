package beam.lang;

import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

/**
 * Parse and load provider implementations during initial parse.
 */
public class BeamProviderLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;

    private static final String PROVIDER = "provider";

    public BeamProviderLoadingListener(BeamVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public void exitResource_block(BeamParser.Resource_blockContext context) {
        if (context.resource_type().getText().equalsIgnoreCase(PROVIDER)) {
            visitor.visitResource_block(context, null);
        }
    }

}
