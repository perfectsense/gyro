package beam.lang;

import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse and load provider implementations during initial parse.
 */
public class ProviderLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private List<Provider> providers;

    private static final String PROVIDER = "provider";

    public ProviderLoadingListener(BeamVisitor visitor) {
        this.visitor = visitor;
    }

    public List<Provider> getProviders() {
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
