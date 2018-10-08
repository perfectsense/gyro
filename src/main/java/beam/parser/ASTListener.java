package beam.parser;

import beam.core.BeamConfigLocation;
import beam.core.BeamObject;
import beam.core.BeamProvider;
import beam.core.BeamResource;
import beam.core.diff.*;
import beam.parser.antlr4.BeamBaseListener;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;

import java.util.*;

public class ASTListener extends BeamBaseListener {

    private ASTBeamRoot root;

    private Set<BeamResource> pending;
    private Map<String, BeamResource> symbolTable;
    private Map<String, BeamProvider> providerTable;
    private final Set<ChangeType> changeTypes = new HashSet<>();
    private Stack<BeamObject> objectStack;

    public ASTBeamRoot getRoot() {
        return root;
    }

    public void setRoot(ASTBeamRoot root) {
        this.root = root;
    }

    @Override
    public void enterBeamRoot(BeamParser.BeamRootContext ctx) {
        pending = new HashSet<>();
        symbolTable = new HashMap<>();
        providerTable = new HashMap<>();
        objectStack = new Stack<>();
    }

    @Override
    public void exitBeamRoot(BeamParser.BeamRootContext ctx) {
        ResourceDiff diff = ASTHandler.exitBeamRoot(pending, changeTypes, providerTable);
        root = new ASTBeamRoot();
        root.setDiff(diff);
    }

    @Override
    public void enterGlobalScope(BeamParser.GlobalScopeContext ctx) {
        super.enterGlobalScope(ctx);
    }

    @Override
    public void enterPluginBlock(BeamParser.PluginBlockContext ctx) {
        String path = ctx.path().getText();
        ASTHandler.fetchPlugin(stripQuotes(path));
    }

    @Override
    public void enterKey(BeamParser.KeyContext ctx) {
        super.enterKey(ctx);
    }

    @Override
    public void enterResourceScope(BeamParser.ResourceScopeContext ctx) {
        super.enterResourceScope(ctx);
    }

    @Override
    public void enterProviderBlock(BeamParser.ProviderBlockContext ctx) {
        String provider = ctx.providerName().PROVIDER_NAME().getSymbol().getText();
        provider = provider.trim();
        String providerName = provider.split("::")[0];
        String resourceKey = provider.split("::")[1];
        String symbol = ctx.resourceSymbol() == null ? null : ctx.resourceSymbol().getText();
        if (symbol != null) {
            symbol = symbol.trim();
        }

        ASTHandler.createBeamObject(providerName, resourceKey, symbol, providerTable, symbolTable, objectStack);
    }

    @Override
    public void exitProviderBlock(BeamParser.ProviderBlockContext ctx) {
        BeamObject object = ASTHandler.loadBeamObject(objectStack, pending);
        for (BeamParser.ResourceScopeContext resourceScopeContext : ctx.resourceScope()) {
            for (BeamParser.KeyValueBlockContext keyValueBlockContext : resourceScopeContext.keyValueBlock()) {
                String key = keyValueBlockContext.key().getText();
                key = key.split(":")[0];
                BeamParser.ValueContext valueContext = keyValueBlockContext.value();
                Object value = null;
                if (valueContext.map() != null) {
                    value = parseMap(valueContext.map());
                } else if (valueContext.list() != null) {
                    value = parseList(valueContext.list());
                } else {
                    String quotedValue = valueContext.getText();
                    quotedValue = quotedValue.replaceAll("^\"|\"$", "");
                    value = quotedValue;
                }

                if (object.getConfigLocation() == null) {
                    int line = keyValueBlockContext.getStart().getLine();
                    int column = keyValueBlockContext.getStart().getCharPositionInLine();
                    object.setConfigLocation(new BeamConfigLocation(line, column));
                }

                ASTHandler.populateSettings(object, key, value, providerTable, symbolTable);
            }
        }
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
            } else {
                result.put(key, value.getText().replaceAll("^\"|\"$", ""));
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
            } else {
                result.add(value.getText().replaceAll("^\"|\"$", ""));
            }
        }

        return result;
    }

    @Override
    public void enterProviderName(BeamParser.ProviderNameContext ctx) {
        super.enterProviderName(ctx);
    }

    @Override
    public void enterValue(BeamParser.ValueContext ctx) {
        super.enterValue(ctx);
    }

    @Override
    public void enterKeyValueBlock(BeamParser.KeyValueBlockContext ctx) {
        super.enterKeyValueBlock(ctx);
    }

    @Override
    public void enterMethod(BeamParser.MethodContext ctx) {
        super.enterMethod(ctx);
    }

    @Override
    public void enterMethodArguments(BeamParser.MethodArgumentsContext ctx) {
        super.enterMethodArguments(ctx);
    }

    @Override
    public void enterMethodNamedArgument(BeamParser.MethodNamedArgumentContext ctx) {
        super.enterMethodNamedArgument(ctx);
    private String stripQuotes(String string) {
        return string.replaceAll("^[\"\']|[\"\']$", "");
    }
}
