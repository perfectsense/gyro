package beam.parser;

import beam.core.*;
import beam.core.diff.*;
import beam.parser.antlr4.BeamBaseListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.*;

public class ASTListener extends BeamBaseListener {

    private ASTBeamRoot root;

    private Set<BeamResource> pending;
    private final Set<ChangeType> changeTypes = new HashSet<>();
    private String configName;
    private BeamContext context;

    public ASTListener(String configName) {
        this.configName = configName;
        this.context = new BeamContext();
    }

    public ASTBeamRoot getRoot() {
        return root;
    }

    public void setRoot(ASTBeamRoot root) {
        this.root = root;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public BeamContext getContext() {
        return context;
    }

    public void setContext(BeamContext context) {
        this.context = context;
    }

    @Override
    public void enterBeamRoot(BeamParser.BeamRootContext ctx) {
        pending = new HashSet<>();
        context = new BeamContext();
    }

    @Override
    public void exitBeamRoot(BeamParser.BeamRootContext ctx) {
        System.out.println(getConfigName());
        BeamContext context = getContext();
        for (String key : context.getContext().keySet()) {
            System.out.println(String.format("%s = %s", key, context.getContext().get(key)));
        }

        System.out.println();
    }

    @Override
    public void enterPluginBlock(BeamParser.PluginBlockContext ctx) {
        String path = ctx.path().getText();
        ASTHandler.fetchPlugin(stripQuotes(path));
    }

    @Override
    public void enterImportBlock(BeamParser.ImportBlockContext ctx) {
        String path = ctx.path().getText();
        path = stripQuotes(path);

        String alias = ctx.variable().getText();

        try {
            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            ASTListener listener = new ASTListener(path);
            ParseTreeWalker.DEFAULT.walk(listener, context);

            BeamContext scopeContext = listener.getContext().scopeContext(alias);
            getContext().getContext().putAll(scopeContext.getContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enterResourceBlock(BeamParser.ResourceBlockContext ctx) {

    }

    @Override
    public void exitResourceBlock(BeamParser.ResourceBlockContext ctx) {
        String resourceProvider = ctx.resourceProvider().RESOURCE_PROVIDER().getSymbol().getText();
        resourceProvider = resourceProvider.trim();
        String packageName = resourceProvider.split("::")[0];
        String resourceKey = resourceProvider.split("::")[1];

        BeamResource resource = (BeamResource) ASTHandler.createBeamObject(packageName, resourceKey);
        for (BeamParser.KeyValueBlockContext keyValueBlockContext : ctx.map().keyValueBlock()) {
            String key = keyValueBlockContext.key().getText();
            ASTHandler.populateSettings(resource, key, parseValueContext(keyValueBlockContext.value()));
        }

        String resourceName = ctx.variable().getText();
        getContext().getContext().put(resourceName, resource);
        System.out.println("unresolved properties: " + resource.getUnResolvedProperties());
    }

    @Override
    public void exitAssignmentBlock(BeamParser.AssignmentBlockContext ctx) {
        String varName = ctx.variable().getText();
        getContext().getContext().put(varName, parseValueContext(ctx.value()));
    }

    private Object parseValueContext(BeamParser.ValueContext valueContext) {
        Object value;
        if (valueContext.map() != null) {
            value = parseMap(valueContext.map());
        } else if (valueContext.list() != null) {
            value = parseList(valueContext.list());
        } else if (valueContext.reference() != null) {
            value = new BeamReference(valueContext.reference().referenceChain().getText());
        } else {
            value = stripQuotes(valueContext.getText());
        }

        return value;
    }

    private Map parseMap(BeamParser.MapContext mapContext) {
        Map result = new HashMap();
        for (BeamParser.KeyValueBlockContext keyValueBlockContext : mapContext.keyValueBlock()) {
            String key = keyValueBlockContext.key().getText();
            key = key.split(":")[0];
            BeamParser.ValueContext value = keyValueBlockContext.value();
            if (value.map() != null) {
                result.put(key, parseMap(value.map()));
            } else if (value.list() != null) {
                result.put(key, parseList(value.list()));
            } else if (value.reference() != null) {
                result.put(key, new BeamReference(value.reference().referenceChain().getText()));
            } else {
                result.put(key, stripQuotes(value.getText()));
            }
        }

        return result;
    }

    private List parseList(BeamParser.ListContext listContext) {
        List result = new ArrayList();
        for (BeamParser.ValueContext value : listContext.value()) {
            if (value.map() != null) {
                result.add(parseMap(value.map()));
            } else if (value.list() != null) {
                result.add(parseList(value.list()));
            } else if (value.reference() != null) {
                result.add(new BeamReference(value.reference().referenceChain().getText()));
            } else {
                result.add(stripQuotes(value.getText()));
            }
        }

        return result;
    }

    private String stripQuotes(String string) {
        return string.replaceAll("^[\"\']|[\"\']$", "");
    }
}
