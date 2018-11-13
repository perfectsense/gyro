package beam.lang;

import beam.parser.antlr4.BeamParser;

public class ConfigExtension implements BeamExtension {

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public void enterExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {

    }

    @Override
    public void exitExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {
        BeamConfig config = new BeamConfig();
        BeamParser.MethodBodyContext methodBodyContext = ctx.methodBody();
        if (methodBodyContext != null) {
            if (methodBodyContext.keyValuePair() != null) {
                for (BeamParser.KeyValuePairContext pairContext: methodBodyContext.keyValuePair()) {
                    config.getContext().put(new BeamConfigKey(null, pairContext.key().getText()), BeamListener.parseValue(pairContext.value()));
                }
            }

            if (methodBodyContext.extension() != null) {
                for (BeamParser.ExtensionContext extensionContext : methodBodyContext.extension()) {
                    String name = extensionContext.extensionName().getText();
                    if (BCL.getExtensions().containsKey(name)) {
                        BeamExtension extension = BCL.getExtensions().get(name);
                        extension.exitExtension(extensionContext, config);
                    } else {
                        throw new BeamLangException(String.format("Unable to load extension %s", name));
                    }
                }
            }
        }

        String id = ctx.param().get(0).getText();
        context.getContext().put(new BeamConfigKey(getName(), id), config);
    }
}
