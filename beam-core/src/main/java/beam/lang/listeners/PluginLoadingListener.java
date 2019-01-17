package beam.lang.listeners;

import beam.lang.BeamVisitor;
import beam.lang.PluginLoader;
import beam.parser.antlr4.BeamParser.PluginContext;
import beam.parser.antlr4.BeamParserBaseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse and load plugins during initial parse.
 */
public class PluginLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private List<PluginLoader> plugins = new ArrayList<>();

    private static final String PROVIDER = "provider";

    public PluginLoadingListener(BeamVisitor visitor) {
        this.visitor = visitor;
    }

    public List<PluginLoader> plugins() {
        return plugins;
    }

    @Override
    public void exitPlugin(PluginContext context) {
        plugins().add(visitor.visitPlugin(context));
    }

}
