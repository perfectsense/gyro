package beam.parser;

import beam.parser.antlr4.BeamBaseListener;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.providerHandler.ProviderHandler;
import org.reflections.Reflections;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class ASTListener extends BeamBaseListener {

    private ASTBeamRoot root;

    public ASTBeamRoot getRoot() {
        return root;
    }

    public void setRoot(ASTBeamRoot root) {
        this.root = root;
    }

    @Override
    public void enterBeamRoot(BeamParser.BeamRootContext ctx) {

    }

    @Override
    public void enterGlobalScope(BeamParser.GlobalScopeContext ctx) {
        super.enterGlobalScope(ctx);
    }

    @Override
    public void enterProviderLocation(BeamParser.ProviderLocationContext ctx) {
        String key = ctx.QUOTED_STRING().getSymbol().getText();
        key = key.replaceAll("^\"|\"$", "");
        Reflections reflections = new Reflections("beam.providerHandler");
        for (Class<? extends ProviderHandler> handlerClass : reflections.getSubTypesOf(ProviderHandler.class)) {
            try {
                ProviderHandler handler = handlerClass.newInstance();
                if (handler.validate(key)) {
                    handler.handle(key);
                }
            } catch (IllegalAccessException | InstantiationException error) {
                error.printStackTrace();
            }
        }
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
        super.enterProviderBlock(ctx);
    }

    @Override
    public void exitProviderBlock(BeamParser.ProviderBlockContext ctx) {
        super.enterProviderBlock(ctx);
        String provider = ctx.providerName().PROVIDER_NAME().getSymbol().getText();
        provider = provider.trim();
        String providerName = provider.split("::")[0];
        String resourceName = provider.split("::")[1];
        try {
            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> resourceClass = loader.loadClass(String.format("beam.%s.%s", providerName, resourceName));
            Object resource = resourceClass.newInstance();
            for (BeamParser.ResourceScopeContext resourceScopeContext : ctx.resourceScope()) {
                for (BeamParser.KeyValueBlockContext keyValueBlockContext : resourceScopeContext.keyValueBlock()) {

                    String key = keyValueBlockContext.key().getText();
                    key = key.split(":")[0];
                    String value = keyValueBlockContext.value().getText();
                    PropertyDescriptor pd = new PropertyDescriptor(key, resourceClass);
                    Method setter = pd.getWriteMethod();
                    setter.invoke(resource, value);
                }
            }

            System.out.println(resource);
        } catch (Exception e) {
            e.printStackTrace();
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
