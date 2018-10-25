package beam.parser;

import beam.core.*;
import beam.core.diff.*;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;
import beam.parser.ast.ASTBeamRoot;
import com.psddev.dari.util.ObjectUtils;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ASTListener extends BeamParserBaseListener {
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
        // need to figure out the import path
        BeamRuntime.getContexts().put(getConfigName(), context);
    }

    @Override
    public void exitBeamRoot(BeamParser.BeamRootContext ctx) {
        List<BeamParser.ImportBlockContext> imports = ctx.globalScope().importBlock();
        if (imports != null) {
            for (BeamParser.ImportBlockContext importBlockContext : imports) {
                // what if already a absolute path?
                String relativePath = importBlockContext.path().getText();
                relativePath = stripQuotes(relativePath);

                File configFile = new File(getConfigName());
                File directory = new File(configFile.getParent());
                File importFile = new File(directory, relativePath);
                try {
                    String path = importFile.getCanonicalPath();
                    BeamContext scopeContext;
                    String alias = importBlockContext.ID().getText();
                    if (!BeamRuntime.getContexts().containsKey(path)) {
                        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
                        CommonTokenStream tokens = new CommonTokenStream(lexer);

                        BeamParser parser = new BeamParser(tokens);
                        BeamParser.BeamRootContext context = parser.beamRoot();

                        ASTListener listener = new ASTListener(path);
                        ParseTreeWalker.DEFAULT.walk(listener, context);

                        scopeContext = listener.getContext().scopeContext(alias);

                    } else {
                        scopeContext = BeamRuntime.getContexts().get(path).scopeContext(alias);
                    }

                    getContext().getContext().putAll(scopeContext.getContext());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void enterPluginBlock(BeamParser.PluginBlockContext ctx) {
        String path = ctx.path().getText();
        ASTHandler.fetchPlugin(stripQuotes(path));
    }

    @Override
    public void enterImportBlock(BeamParser.ImportBlockContext ctx) {
    }

    @Override
    public void enterResourceBlock(BeamParser.ResourceBlockContext ctx) {

    }

    @Override
    public void exitResourceBlock(BeamParser.ResourceBlockContext ctx) {
        String resourceProvider = ctx.RESOURCE_PROVIDER().getSymbol().getText();
        resourceProvider = resourceProvider.trim();
        String packageName = resourceProvider.split("::")[0];
        String resourceKey = resourceProvider.split("::")[1];

        BeamResource resource = (BeamResource) ASTHandler.createBeamObject(packageName, resourceKey);
        for (BeamParser.KeyValueBlockContext keyValueBlockContext : ctx.resourceBody().keyValueBlock()) {
            String key = keyValueBlockContext.key().getText();
            resource.getUnResolvedProperties().put(key, parseValueContext(keyValueBlockContext.value()));
        }

        String resourceName = ctx.ID().getText();
        BeamContextKey key = new BeamContextKey(resourceProvider, resourceName);
        getContext().getNativeContext().put(key, resource);
        getContext().getContext().put(key, resource);
    }

    @Override
    public void exitAssignmentBlock(BeamParser.AssignmentBlockContext ctx) {
        String varName = ctx.VARIABLE().getText();
        BeamContextKey key = new BeamContextKey(BeamContextKey.CONSTANT_KEY, varName);
        BeamReferable referable = parseValueContext(ctx.value());
        getContext().getNativeContext().put(key, referable);
        getContext().getContext().put(key, referable);
    }

    private BeamReferable parseValueContext(BeamParser.ValueContext valueContext) {
        if (valueContext.map() != null) {
            return parseMap(valueContext.map());
        } else if (valueContext.list() != null) {
            return parseList(valueContext.list());
        } else if (valueContext.scalar() != null) {
            return parseScalar(valueContext.scalar());
        } else {
            throw new IllegalStateException();
        }
    }

    private BeamMap parseMap(BeamParser.MapContext mapContext) {
        BeamMap result = new BeamMap();
        for (BeamParser.MapEntryContext mapEntryContext : mapContext.mapEntry()) {
            BeamParser.KeyScalarBlockContext keyScalarBlockContext = mapEntryContext.keyScalarBlock();
            String key = keyScalarBlockContext.mapKey().getText();
            BeamParser.ScalarContext value = keyScalarBlockContext.scalar();
            result.getMap().put(key, parseScalar(value));
        }

        return result;
    }

    private BeamList parseList(BeamParser.ListContext listContext) {
        BeamList result = new BeamList();
        for (BeamParser.ListEntryContext listEntryContext : listContext.listEntry()) {
            BeamParser.ScalarContext item = listEntryContext.scalar();
            result.getList().add(parseScalar(item));
        }

        return result;
    }

    private BeamScalar parseScalar(BeamParser.ScalarContext scalarContext) {
        BeamScalar beamScalar = new BeamScalar();
        BeamParser.ScalarLiteralContext scalarLiteralContext = scalarContext.scalarFirstLiteral().scalarLiteral();
        if (scalarLiteralContext.reference() != null) {
            beamScalar.getElements().add(parseReference(scalarLiteralContext.reference()));

        } else if (scalarLiteralContext.unquotedLiteral() != null) {
            beamScalar.getElements().add(new BeamLiteral(scalarLiteralContext.unquotedLiteral().getText()));
        }

        if (scalarContext.scalarRestLiterals() != null) {
            for (BeamParser.ScalarLiteralContext literalContext : scalarContext.scalarRestLiterals().scalarLiteral()) {
                if (literalContext.reference() != null) {
                    beamScalar.getElements().add(parseReference(literalContext.reference()));

                } else if (literalContext.unquotedLiteral() != null) {
                    StringBuilder sb = new StringBuilder();
                    for (TerminalNode node : literalContext.WS()) {
                        sb.append(node.getText());
                    }

                    sb.append(literalContext.unquotedLiteral().getText());
                    beamScalar.getElements().add(new BeamLiteral(sb.toString()));
                }
            }
        }

        return beamScalar;
    }

    private BeamReference parseReference(BeamParser.ReferenceContext referenceContext) {
        if (referenceContext.constantReference() != null) {
            String type = BeamContextKey.CONSTANT_KEY;
            String id = referenceContext.constantReference().constantReferenceChain().getText();
            return new BeamReference(new BeamContextKey(type, id), new ArrayList<>());

        } else if (referenceContext.tagReference() != null) {
            String type = referenceContext.tagReference().RESOURCE_PROVIDER().getText();
            String id = referenceContext.tagReference().referenceChain().get(0).getText();
            String referenceChain = referenceContext.tagReference().referenceChain().get(1).getText();
            return new BeamReference(new BeamContextKey(type, id), referenceChain);

        } else if (referenceContext.resourceReference() != null) {
            String type = referenceContext.resourceReference().RESOURCE_PROVIDER().getText();
            String id = referenceContext.resourceReference().referenceChain().get(0).getText();
            String referenceChain = referenceContext.resourceReference().referenceChain().get(1).getText();
            return new BeamReference(new BeamContextKey(type, id), referenceChain);

        } else {
            throw new IllegalStateException();
        }
    }

    private String stripQuotes(String string) {
        return string.replaceAll("^[\"\']|[\"\']$", "");
    }
}
