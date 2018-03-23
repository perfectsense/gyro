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
        ASTHandler.exitBeamRoot(pending, changeTypes, providerTable);
    }

    @Override
    public void enterGlobalScope(BeamParser.GlobalScopeContext ctx) {
        super.enterGlobalScope(ctx);
    }

    @Override
    public void enterProviderLocation(BeamParser.ProviderLocationContext ctx) {
        String key = ctx.QUOTED_STRING().getSymbol().getText();
        key = key.replaceAll("^\"|\"$", "");
        ASTHandler.enterProviderLocation(key);
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
                String value = keyValueBlockContext.value().getText();
                if (object.getConfigLocation() == null) {
                    int line = keyValueBlockContext.getStart().getLine();
                    int column = keyValueBlockContext.getStart().getCharPositionInLine();
                    object.setConfigLocation(new BeamConfigLocation(line, column));
                }

                ASTHandler.populateSettings(object, key, value, providerTable, symbolTable);
            }
        }
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
    }
}
