package beam.lang;

import beam.parser.antlr4.BeamParser;

public class ForExtension implements BeamExtension {

    @Override
    public String getName() {
        return "for";
    }

    @Override
    public void enterExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {

    }

    @Override
    public void exitExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {
        BeamList list = BeamListener.parseInlineList(ctx.param().get(2).inlineList());
        BeamLiteral var = BeamListener.parseLiteral(ctx.param().get(0).literal());
        var.resolve(context);
        list.resolve(context);

        BeamResolvable resolvable = context.get(var.getLiteral());

        for (BeamResolvable iter : list.getList()) {
            context.getContext().put(new BeamConfigKey(null, var.getLiteral()), iter);
            BeamParser.MethodBodyContext methodBodyContext = ctx.methodBody();
            if (methodBodyContext != null) {
                if (methodBodyContext.extension() != null) {
                    for (BeamParser.ExtensionContext extensionContext : methodBodyContext.extension()) {
                        String name = extensionContext.extensionName().getText();
                        if (BCL.getExtensions().containsKey(name)) {
                            BeamExtension extension = BCL.getExtensions().get(name);
                            extension.exitExtension(extensionContext, context);
                        } else {
                            throw new BeamLangException(String.format("Unable to load extension %s", name));
                        }
                    }
                }
            }
        }

        if (resolvable != null) {
            context.getContext().put(new BeamConfigKey(null, var.getLiteral()), resolvable);
        } else {
            context.getContext().remove(new BeamConfigKey(null, var.getLiteral()));
        }
    }
}
