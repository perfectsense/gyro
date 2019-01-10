package beam.lang;

import beam.core.BeamProvider;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse and load provider implementations during initial parse.
 */
public class BeamProviderLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private List<BeamProvider> providers;

    private static final String PROVIDER = "provider";

    public BeamProviderLoadingListener(BeamVisitor visitor) {
        this.visitor = visitor;
    }

    public List<BeamProvider> getProviders() {
        if (providers == null) {
            providers = new ArrayList<>();
        }

        return providers;
    }

    @Override
    public void exitProvider_block(BeamParser.Provider_blockContext context) {
        getProviders().add(visitor.visitProvider_block(context));
    }

}
